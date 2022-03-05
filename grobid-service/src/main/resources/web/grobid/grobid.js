/**
*  Javascript functions for the front end.
*
*  Author: Patrice Lopez
*/

//jQuery.fn.prettify = function () { this.html(prettyPrintOne(this.html(),'xml')); };

var grobid = (function($) {

	var teiToDownload;
	var teiPatentToDownload;

	var block = 0;

    var elementCoords = ['s', 'biblStruct', 'persName', 'figure', 'formula', 'head'];

	function defineBaseURL(ext) {
		var baseUrl = null;
        var localBase = $(location).attr('href');
		if ( localBase.indexOf("index.html") != -1) {
            localBase = localBase.replace("index.html", "");
        } 
        if (localBase.endsWith("#")) {
            localBase = localBase.substring(0,localBase.length-1);
        } 
		return localBase + "api/" + ext;
	}

	function setBaseUrl(ext) {
		var baseUrl = defineBaseURL(ext);
		if (block == 0)
			$('#gbdForm').attr('action', baseUrl);
		else if (block == 1)
			$('#gbdForm2').attr('action', baseUrl);
		else if (block == 2)
			$('#gbdForm3').attr('action', baseUrl);
	}

	$(document).ready(function() {
		$("#subTitle").html("API");
		$("#divAbout").show();
		//$("#divAdmin").hide();

		// for TEI-based results
        $("#divRestI").hide();

        // for PDF based results
        $("#divRestII").hide();

        // for patent processing
        $("#divRestIII").hide();

		$("#divDoc").hide();
		$('#consolidateBlock').show();
        $("#btn_download").hide();
        $("#btn_download3").hide();

		createInputFile();
		createInputFile2();
		createInputFile3();
		setBaseUrl('processHeaderDocument');
		block = 0;

        $('#api_file').change((e) => {
            $('.file-list-li').remove()
            $(e.target.files).each((i, elem) => {
                let li = document.createElement("li");
                li.className = "file-list-li"
                li.innerText = elem.name;
                $('#file-list').append(li);
            })
        })

		$('#selectedService').change(function() {
			processChange();
			return true;
		});

		$('#selectedService2').change(function() {
			processChange();
			return true;
		});

		$('#selectedService3').change(function() {
			processChange();
			return true;
		});

		$('#gbdForm').ajaxForm({
            beforeSubmit: ShowRequest1,
            success: SubmitSuccesful,
            error: AjaxError1,
            dataType: "text"
        });

		$('#submitRequest2').bind('click', submitQuery2);
		$('#submitRequest3').bind('click', submitQuery3);
		$('#submitRequest4').bind('click', submitQuery4);

		// bind download buttons with download methods
		$('#btn_download').bind('click', download);
		$("#btn_download").hide();
		$('#btn_download3').bind('click', downloadPatent);
		$("#btn_download3").hide();
        $('#btn_block_1').bind('click', downloadVisibilty);
        $('#btn_block_3').bind('click', downloadVisibilty3);
		//$('#adminForm').attr("action", defineBaseURL("allProperties"));
		//$('#TabAdminProps').hide();
		/*$('#adminForm').ajaxForm({
	        beforeSubmit: adminShowRequest,
	        success: adminSubmitSuccesful,
	        error: adminAjaxError,
	        dataType: "text"
	        });*/

        $('.pdf_button').click(function() {
            if ((document.getElementById("api_file").files[0].type == 'application/pdf') ||
                (document.getElementById("api_file").files[0].name.endsWith(".pdf")) ||
                (document.getElementById("api_file").files[0].name.endsWith(".PDF"))) {
                var reader = new FileReader();
                reader.onloadend = function () {
                    let pdfAsArray = new Uint8Array(reader.result);
                    let pdf_window = window.open("", "pdf");
                    let window_div = pdf_window.document.createElement("div")
                    window_div.id = "requestResult2";
                    pdf_window.document.body.appendChild(window_div);
                    var container = pdf_window.document.getElementById("requestResult2");

                    // Use PDFJS to render a pdfDocument from pdf array
                    // var frame = '<iframe id="pdfViewer" src="resources/pdf.js/web/viewer.html?file=" style="width: 100%; height: 1000px;"></iframe>';
                    $(window_div).html();
                    let frame = $("<iframe id='myId'></iframe>");
                    frame.attr('src', "resources/pdf.js/web/viewer.html?file=");
                                    // .attr('src', "resources/pdf.js/web/viewer.html?file=")
                                    // .attr('width', '100%')
                                    // .attr('height', '1000px')
                                    // .onload(function()
                                    // {
                                    //     console.log(pdfAsArray)
                                    // });
                    $(window_div).append(frame)


                    // var frame = pdf_window.document.createElement("iframe");
                    // frame.src = "resources/pdf.js/web/viewer.html?file=";
                    // frame.style.width = "100%";
                    // frame.style.height = "1000px";
                    // frame.onload = function () {
                    //     console.log(pdfAsArray)
                    //     console.log(this.contentWindow)
                    //     this.contentWindow.PDFViewerApplication.open(pdfAsArray);
                    // }
                    //
                    // pdf_window.window.document.body.appendChild(frame);

                    // var pdfjsframe = pdf_window.document.getElementById('pdfViewer');
                    // pdfjsframe.onloadend = function () {
                    //     console.log(pdfAsArray)
                    //     pdfjsframe.contentWindow.PDFViewerApplication.open(pdfAsArray);
                    // }
                }
                reader.readAsArrayBuffer(document.getElementById("api_file").files[0]);
            }
        })

		$("#about").click(function() {
			$("#about").attr('class', 'section-active');
			$("#rest").attr('class', 'section-not-active');
			$("#pdf").attr('class', 'section-not-active');
			//$("#admin").attr('class', 'section-not-active');
			$("#doc").attr('class', 'section-not-active');
			$("#patent").attr('class', 'section-not-active');

			$("#subTitle").html("API");
			$("#subTitle").show();

			$("#divAbout").show();
			$("#divRestI").hide();
			$("#divRestII").hide();
			$("#divRestIII").hide();
			//$("#divAdmin").hide();
			$("#divDoc").hide();
			$("#divDemo").hide();
			return false;
		});
		$("#rest").click(function() {
			$("#rest").attr('class', 'section-active');
			$("#pdf").attr('class', 'section-not-active');
			$("#doc").attr('class', 'section-not-active');
			$("#about").attr('class', 'section-not-active');
			//$("#admin").attr('class', 'section-not-active');
			$("#patent").attr('class', 'section-not-active');

			$("#subTitle").hide();
			block = 0;
			//$("#subTitle").html("TEI output service");
			//$("#subTitle").show();
			processChange();

			$("#divRestI").show();
			$("#divRestII").hide();
			$("#divRestIII").hide();
			$("#divAbout").hide();
			$("#divDoc").hide();
			//$("#divAdmin").hide();
			$("#divDemo").hide();
			return false;
		});
		/*$("#admin").click(function() {
			$("#admin").attr('class', 'section-active');
			$("#doc").attr('class', 'section-not-active');
			$("#about").attr('class', 'section-not-active');
			$("#rest").attr('class', 'section-not-active');
			$("#pdf").attr('class', 'section-not-active');
			$("#patent").attr('class', 'section-not-active');

			$("#subTitle").html("Admin");
			$("#subTitle").show();
			setBaseUrl('admin');

			$("#divRestI").hide();
			$("#divRestII").hide();
			$("#divRestIII").hide();
			$("#divAbout").hide();
			$("#divDoc").hide();
			//$("#divAdmin").show();
			$("#divDemo").hide();
			return false;
		});*/
		$("#doc").click(function() {
			$("#doc").attr('class', 'section-active');
			$("#rest").attr('class', 'section-not-active');
			$("#pdf").attr('class', 'section-not-active');
			$("#patent").attr('class', 'section-not-active');
			$("#about").attr('class', 'section-not-active');
			//$("#admin").attr('class', 'section-not-active');

			$("#subTitle").html("Doc");
			$("#subTitle").show();

			$("#divDoc").show();
			$("#divAbout").hide();
			$("#divRestI").hide();
			$("#divRestII").hide();
			$("#divRestIII").hide();
			//$("#divAdmin").hide();
			$("#divDemo").hide();
			return false;
		});
		$("#pdf").click(function() {
			$("#pdf").attr('class', 'section-active');
			$("#rest").attr('class', 'section-not-active');
			$("#patent").attr('class', 'section-not-active');
			$("#about").attr('class', 'section-not-active');
			//$("#admin").attr('class', 'section-not-active');
			$("#doc").attr('class', 'section-not-active');

			block = 1;
			setBaseUrl('referenceAnnotations');
			$("#subTitle").hide();
			processChange();
			//$("#subTitle").html("PDF annotation services");
			//$("#subTitle").show();

			$("#divDoc").hide();
			$("#divAbout").hide();
			$("#divRestI").hide();
			$("#divRestII").show();
			$("#divRestIII").hide();
			//$("#divAdmin").hide();
			return false;
		});
		$("#patent").click(function() {
			$("#patent").attr('class', 'section-active');
			$("#rest").attr('class', 'section-not-active');
			$("#pdf").attr('class', 'section-not-active');
			$("#about").attr('class', 'section-not-active');
			//$("#admin").attr('class', 'section-not-active');
			$("#doc").attr('class', 'section-not-active');

			block = 2;
			setBaseUrl('processCitationPatentST36');
			$("#subTitle").hide();
			processChange();

			$("#divDoc").hide();
			$("#divAbout").hide();
			$("#divRestI").hide();
			$("#divRestII").hide();
			$("#divRestIII").show();
			//$("#divAdmin").hide();
			return false;
		});
	});

	function ShowRequest1(formData, jqForm, options) {
        var addCoordinates = false;
        for(var formd in formData) {
            if (formData[formd].name == 'teiCoordinates') {
                addCoordinates = true;
            }
        }
        if (addCoordinates) {
            for (var i in elementCoords) {
                var additionalFormData = {
                    "name": "teiCoordinates",
                    "value": "ref",
                    "type": "checkbox",
                    "required": false
                }
                additionalFormData["value"] = elementCoords[i]
                formData.push(additionalFormData)
            }
        }
	    $('#requestResult').html('<font color="grey">Requesting server...</font>');
	    return true;
	}

	function ShowRequest2(formData, jqForm, options) {
	    $('#infoResult2').html('<font color="grey">Requesting server...</font>');
	    return true;
	}

	function ShowRequest3(formData, jqForm, options) {
	    $('#requestResult3').html('<font color="grey">Requesting server...</font>');
	    return true;
	}

	function AjaxError1(jqXHR, textStatus, errorThrown) {
		$('#requestResult').html("<font color='red'>Error encountered while requesting the server.<br/>"+jqXHR.responseText+"</font>");
		responseJson = null;
	}

    function AjaxError2(message) {
    	if (!message)
    		message ="";
    	message += " - The PDF document cannot be annotated. Please check the server logs.";
    	$('#infoResult2').html("<font color='red'>Error encountered while requesting the server.<br/>"+message+"</font>");
		responseJson = null;
        return true;
    }

    function AjaxError21(message) {
        if (!message)
            message ="";
        $('#infoResult2').html("<font color='red'>Error encountered while requesting the server.<br/>"+message+"</font>");
        responseJson = null;
        return true;
    }

	function AjaxError3(jqXHR, textStatus, errorThrown) {
		$('#requestResult3').html("<font color='red'>Error encountered while requesting the server.<br/>"+jqXHR.responseText+"</font>");
		responseJson = null;
	}

	function htmll(s) {
    	return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  	}

	function SubmitSuccesful(responseText, statusText, xhr) {
		//var selected = $('#selectedService option:selected').attr('value');
		var display = "<pre class='prettyprint lang-xml' id='xmlCode'>";
		var testStr = vkbeautify.xml(responseText);
        teiToDownload = responseText;
		display += htmll(testStr);

		display += "</pre>";
		$('#requestResult').html(display);
		window.prettyPrint && prettyPrint();
		$('#requestResult').show();
        $("#btn_download").show();
	}

    function submitQuery2() {
        var selected = $('#selectedService2 option:selected').attr('value');
        if (selected == 'annotatePDF') {
            // we will have a PDF back
            //PDFJS.disableWorker = true;

            var form = document.getElementById('gbdForm2');
            var formData = new FormData(form);
            var xhr = new XMLHttpRequest();
            var url = $('#gbdForm2').attr('action');
            xhr.responseType = 'arraybuffer';
            xhr.open('POST', url, true);
            ShowRequest2();
            xhr.onreadystatechange = function (e) {
                if (xhr.readyState == 4) {
                    if (xhr.status == 200) {
                        var response = e.target.response;
                        var pdfAsArray = new Uint8Array(response);
                        // Use PDFJS to render a pdfDocument from pdf array
                        var frame = '<iframe id="pdfViewer" src="resources/pdf.js/web/viewer.html?file=" style="width: 100%; height: 1000px;"></iframe>';
                        $('#requestResult2').html();
                        $('#infoResult2').html(frame);
                        var pdfjsframe = document.getElementById('pdfViewer');
                        pdfjsframe.onload = function () {
                            pdfjsframe.contentWindow.PDFViewerApplication.open(pdfAsArray);
                        };
                    } else {
                        //AjaxError2("Response " + xhr.status + ": " + xhr.responseText);
                        AjaxError2("Response " + xhr.status + ": " );
                    }
                }
            };
            xhr.send(formData);  // multipart/form-data
        } else {
            // we will have JSON annotations to be layered on the PDF

            // request for the annotation information
            var form = document.getElementById('gbdForm2');
            var formData = new FormData(form);
            var xhr = new XMLHttpRequest();
            var url = $('#gbdForm2').attr('action');
            xhr.responseType = 'json';
            xhr.open('POST', url, true);
            ShowRequest2();

            var nbPages = -1;

            // display the local PDF
            if ((document.getElementById("input2").files[0].type == 'application/pdf') ||
                (document.getElementById("input2").files[0].name.endsWith(".pdf")) ||
                (document.getElementById("input2").files[0].name.endsWith(".PDF"))) {
                var reader = new FileReader();
                reader.onloadend = function () {
                    // to avoid cross origin issue
                    //PDFJS.disableWorker = true;
                    var pdfAsArray = new Uint8Array(reader.result);
                    // Use PDFJS to render a pdfDocument from pdf array
                    PDFJS.getDocument(pdfAsArray).then(function (pdf) {
                        // Get div#container and cache it for later use
                        var container = document.getElementById("requestResult2");
                        // enable hyperlinks within PDF files.
                        //var pdfLinkService = new PDFJS.PDFLinkService();
                        //pdfLinkService.setDocument(pdf, null);

                        $('#requestResult2').html('');
                        nbPages = pdf.numPages;

                        // Loop from 1 to total_number_of_pages in PDF document
                        for (var i = 1; i <= nbPages; i++) {

                            // Get desired page
                            pdf.getPage(i).then(function (page) {

                                var div0 = document.createElement("div");
                                div0.setAttribute("style", "text-align: center; margin-top: 1cm;");
                                var pageInfo = document.createElement("p");
                                var t = document.createTextNode("page " + (page.pageIndex + 1) + "/" + (nbPages));
                                pageInfo.appendChild(t);
                                div0.appendChild(pageInfo);
                                container.appendChild(div0);

                                var scale = 1.5;
                                var viewport = page.getViewport(scale);
                                var div = document.createElement("div");

                                // Set id attribute with page-#{pdf_page_number} format
                                div.setAttribute("id", "page-" + (page.pageIndex + 1));

                                // This will keep positions of child elements as per our needs, and add a light border
                                div.setAttribute("style", "position: relative; border-style: solid; border-width: 1px; border-color: gray;");

                                // Append div within div#container
                                container.appendChild(div);

                                // Create a new Canvas element
                                var canvas = document.createElement("canvas");

                                // Append Canvas within div#page-#{pdf_page_number}
                                div.appendChild(canvas);

                                var context = canvas.getContext('2d');
                                canvas.height = viewport.height;
                                canvas.width = viewport.width;

                                var renderContext = {
                                    canvasContext: context,
                                    viewport: viewport
                                };

                                // Render PDF page
                                page.render(renderContext).then(function () {
                                    // Get text-fragments
                                    return page.getTextContent();
                                })
                                    .then(function (textContent) {
                                        // Create div which will hold text-fragments
                                        var textLayerDiv = document.createElement("div");

                                        // Set it's class to textLayer which have required CSS styles
                                        textLayerDiv.setAttribute("class", "textLayer");

                                        // Append newly created div in `div#page-#{pdf_page_number}`
                                        div.appendChild(textLayerDiv);

                                        // Create new instance of TextLayerBuilder class
                                        var textLayer = new TextLayerBuilder({
                                            textLayerDiv: textLayerDiv,
                                            pageIndex: page.pageIndex,
                                            viewport: viewport
                                        });

                                        // Set text-fragments
                                        textLayer.setTextContent(textContent);

                                        // Render text-fragments
                                        textLayer.render();
                                    });
                            });
                        }
                    });
                }
                reader.readAsArrayBuffer(document.getElementById("input2").files[0]);

                xhr.onreadystatechange = function (e) {
                    if (xhr.readyState == 4 && xhr.status == 200) {
                        var response = e.target.response;
                        //var response = JSON.parse(xhr.responseText);
                        //console.log(response);
                        setupAnnotations(response);
                    } else if (xhr.status != 200) {
                        AjaxError2("Response " + xhr.status + ": ");
                    }
                };
                xhr.send(formData);
            } else {
                AjaxError21("This does not look like a PDF");
            }
        }
    }


	function submitQuery3() {
		var selected = $('#selectedService3 option:selected').attr('value');
		if (selected == 'citationPatentPDFAnnotation') {
			// we will have a PDF back
			//PDFJS.disableWorker = true;

			var form = document.getElementById('gbdForm3');
			var formData = new FormData(form);
			var xhr = new XMLHttpRequest();
			var url = $('#gbdForm3').attr('action');
			xhr.responseType = 'arraybuffer';
			xhr.open('POST', url, true);
			ShowRequest3();
			xhr.onreadystatechange = function(e) {
				if (xhr.readyState == 4 && xhr.status == 200) {
				    var response = e.target.response;
				    var pdfAsArray = new Uint8Array(response);
					// Use PDFJS to render a pdfDocument from pdf array
					var frame = '<iframe id="pdfViewer" src="resources/pdf.js/web/viewer.html?file=" style="width: 100%; height: 1000px;"></iframe>';
					$('#requestResult3').html(frame);
					var pdfjsframe = document.getElementById('pdfViewer');
					pdfjsframe.onload = function() {
						pdfjsframe.contentWindow.PDFViewerApplication.open(pdfAsArray);
					};
				} else  if (xhr.status != 200) {
					AjaxError3(xhr);
				}
			};
			xhr.send(formData);  // multipart/form-data
		} else if (selected == 'citationPatentAnnotations') {
			// we will have JSON annotations to be layered on the PDF

			// request for the annotation information
			var form = document.getElementById('gbdForm3');
			var formData = new FormData(form);
			var xhr = new XMLHttpRequest();
			var url = $('#gbdForm3').attr('action');
			xhr.responseType = 'json';
			xhr.open('POST', url, true);
			ShowRequest3();

			var nbPages = -1;

			// display the local PDF

			if ( (document.getElementById("input3").files[0].type == 'application/pdf') ||
			   	 (document.getElementById("input3").files[0].name.endsWith(".pdf")) ||
				 (document.getElementById("input3").files[0].name.endsWith(".PDF")) ) {
                var reader = new FileReader();
                reader.onloadend = function () {
					// to avoid cross origin issue
					//PDFJS.disableWorker = true;
				    var pdfAsArray = new Uint8Array(reader.result);
					// Use PDFJS to render a pdfDocument from pdf array
				    PDFJS.getDocument(pdfAsArray).then(function (pdf) {
				        // Get div#container and cache it for later use
			            var container = document.getElementById("requestResult3");
			            // enable hyperlinks within PDF files.
			            //var pdfLinkService = new PDFJS.PDFLinkService();
			            //pdfLinkService.setDocument(pdf, null);

						$('#requestResult3').html('');
						nbPages = pdf.numPages;

			            // Loop from 1 to total_number_of_pages in PDF document
			            for (var i = 1; i <= nbPages; i++) {

			                // Get desired page
			                pdf.getPage(i).then(function(page) {

							  	var div0 = document.createElement("div");
							  	div0.setAttribute("style", "text-align: center; margin-top: 1cm;");
			                  	var pageInfo = document.createElement("p");
			                  	var t = document.createTextNode("page " + (page.pageIndex + 1) + "/" + (nbPages));
							  	pageInfo.appendChild(t);
							  	div0.appendChild(pageInfo);
			                  	container.appendChild(div0);

			                  	var scale = 1.5;
			                 	var viewport = page.getViewport(scale);
				                var div = document.createElement("div");

			                  	// Set id attribute with page-#{pdf_page_number} format
			                  	div.setAttribute("id", "page-" + (page.pageIndex + 1));

			                  	// This will keep positions of child elements as per our needs, and add a light border
			                  	div.setAttribute("style", "position: relative; border-style: solid; border-width: 1px; border-color: gray;");

			                  	// Append div within div#container
			                  	container.appendChild(div);

			                  	// Create a new Canvas element
			                  	var canvas = document.createElement("canvas");

			                  	// Append Canvas within div#page-#{pdf_page_number}
			                  	div.appendChild(canvas);

			                  	var context = canvas.getContext('2d');
			                  	canvas.height = viewport.height;
			                  	canvas.width = viewport.width;

			                  	var renderContext = {
			                    	canvasContext: context,
			                  		viewport: viewport
			                  	};

			                  	// Render PDF page
			                  	page.render(renderContext).then(function() {
			                        // Get text-fragments
			                        return page.getTextContent();
			                    })
			                    .then(function(textContent) {
			                        // Create div which will hold text-fragments
			                        var textLayerDiv = document.createElement("div");

			                        // Set it's class to textLayer which have required CSS styles
			                        textLayerDiv.setAttribute("class", "textLayer");

			                        // Append newly created div in `div#page-#{pdf_page_number}`
			                        div.appendChild(textLayerDiv);

			                        // Create new instance of TextLayerBuilder class
			                        var textLayer = new TextLayerBuilder({
			                          textLayerDiv: textLayerDiv,
			                          pageIndex: page.pageIndex,
			                          viewport: viewport
			                        });

			                        // Set text-fragments
			                        textLayer.setTextContent(textContent);

			                        // Render text-fragments
			                        textLayer.render();
			                    });
			                });
			            }
				    });
				}
				reader.readAsArrayBuffer(document.getElementById("input3").files[0]);
			}

			xhr.onreadystatechange = function(e) {
				if (xhr.readyState == 4 && xhr.status == 200) {
				    var response = e.target.response;
				    //var response = JSON.parse(xhr.responseText);
				 	//console.log(response);
				    setupPatentAnnotations(response);
				} else  if (xhr.status != 200) {
					AjaxError3(xhr);
				}
			};
			xhr.send(formData);
		} else {
			// request for extraction, returning TEI result
			var xhr = new XMLHttpRequest();
			var url = $('#gbdForm3').attr('action');
			xhr.responseType = 'xml';
			xhr.onreadystatechange = function(e) {
				if (xhr.readyState == 4 && xhr.status == 200) {
				    var response = e.target.response;
				    //var response = JSON.parse(xhr.responseText);
				 	//console.log(response);

				} else if (xhr.status != 200) {
					AjaxError3(xhr);
				}
			};

			if (document.getElementById("input3").files && 
				document.getElementById("input3").files.length >0 &&
				!$('#textInputDiv3').is(":visible")) {
				var formData = new FormData();

				var url = $('#gbdForm3').attr('action');

				var formData = new FormData();
				formData.append('input', document.getElementById("input3").files[0]);
				
				if ($("#consolidate3").is(":checked"))	
					formData.append('consolidateCitations', 1);
				else
					formData.append('consolidateCitations', 0);
				
				xhr.open('POST', url, true);
				ShowRequest3();

				xhr.send(formData);
			} else if ($('#textInputDiv3').is(":visible")) {
				var params = 'input='+encodeURIComponent($("#textInputArea3").val());
				if ($("#consolidate3").is(":checked"))	
					params += '&consolidateCitations=1';
				else 
					params += '&consolidateCitations=0';

				xhr.open('POST', url, true);
				xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
				ShowRequest3();

				xhr.send(params);
			}
		}
	}

    function submitQuery4(){
        if(document.getElementById("api_file").files.length > 0){
            $('.file-list-li').each((i,e) => {
                $(e).addClass("li-processing")
            })
            var form = document.getElementById('apiForm');
            var formData = new FormData(form);
            var xhr = new XMLHttpRequest();
            var url = "/api/getMeta"
            xhr.responseType = 'json';
            xhr.open('POST', url, true);
            ShowRequest2();
            xhr.onreadystatechange = function (e) {
                if (xhr.readyState == 4) {
                    if (xhr.status == 200) {
                        // document.getElementById("json-viewer").innerText = '';
                        let response = e.target.response;
                        // $('#json-viewer').jsonViewer(response);
                        $('.file-list-li').each((i,e)=>{
                            let area = document.createElement("div");
                            let img_viewer = document.createElement("div");
                            // let spinner = document.createElement("div");
                            let json_viewer = document.createElement("pre");
                            let pdf_button = document.createElement("button");

                            let figures = response[i].figures;
                            let tables = response[i].tables;
                            let assetPath = response[i].assetPath;

                            console.log(figures);
                            console.log(tables);
                            console.log(assetPath);

                            // spinner.className = 'loading'
                            for(let i=0; i<figures.length; i++){
                                let preview = document.createElement("div");
                                let caption = document.createElement("small");
                                let image = figures[i].bitmapGraphicObjects;
                                if(image){
                                    if(image.length === 1){
                                        getImage(assetPath + "/" + image[0].uri.split("/")[1], i);
                                    }else{
                                        console.log('more than one image')
                                    }
                                    preview.className = "preview-img";
                                    preview.id = "preview-box-"+i;
                                    caption.className = "preview-caption";
                                    caption.innerText = figures[i].caption.replaceAll("\n", "");
                                    preview.onclick = (e) => {
                                        e.stopPropagation()
                                        let figureWin = window.open("", "image_popup", "width=" + e.target.naturalWidth + ", height=" + (Number(e.target.naturalHeight) + 40));
                                        figureWin.document.write("<html><body style='margin:0'>")
                                        figureWin.document.write("<a href=javascript:window.close()><img src='" + e.target.src + "' border=0></a><br/>");
                                        figureWin.document.write("<small style='font-size: 25px'>"+ caption.innerText +"</small>")
                                        figureWin.document.write("</body><html>");
                                    }

                                    preview.append(caption);
                                    img_viewer.append(preview);
                                }
                            }
                            let file = document.getElementById("api_file").files[i];

                            delete response[i].figures;
                            delete response[i].tables;
                            delete response[i].assetPath;

                            const removeEmptyOrNull = (obj) => {
                                Object.keys(obj).forEach(k =>
                                    (obj[k] && typeof obj[k] === 'object') && removeEmptyOrNull(obj[k]) ||
                                    (!obj[k] && obj[k] !== undefined) && delete obj[k]
                                );
                                return obj;
                            };

                            let res = removeEmptyOrNull(response[i])

                            area.style.display = 'none';
                            $(json_viewer).jsonViewer(res);
                            img_viewer.id = "img_viewer_"+i
                            img_viewer.className = "img_viewer"

                            for(let k=0; k<tables.length; k++){

                                let preview = document.createElement("div");
                                let caption = document.createElement("small");
                                let header = document.createElement("small");
                                caption.className = "preview-caption";
                                caption.innerText = tables[k].caption;
                                header.className = "preview-caption";
                                header.innerText = tables[k].header;
                                let textArea = tables[k].textArea
                                for(let j=0; j<textArea.length; j++){
                                    let fileReader = new FileReader();
                                    fileReader.onload =(ev) => {
                                        PDFJS.getDocument(fileReader.result).then((pdf) => {
                                            pdf.getPage(textArea[j].page)
                                                .then((page) => {
                                                    let scale = 2;
                                                    let viewport = page.getViewport(scale);
                                                    let canvas = document.createElement('canvas');
                                                    let context = canvas.getContext("2d");
                                                    canvas.height = viewport.height;
                                                    canvas.width = viewport.width;
                                                    let task = page.render({canvasContext: context, viewport: viewport})
                                                    task.promise.then(() => {
                                                        // let pageImage = new Image();
                                                        // pageImage.src = canvas.toDataURL('image/png');
                                                        let resultCanvas = document.createElement('canvas');
                                                        let resultContext = resultCanvas.getContext('2d');
                                                        resultCanvas.width = (textArea[j].width*2)+10;
                                                        resultCanvas.height = (textArea[j].height*2)+10;
                                                        resultContext.drawImage(canvas, textArea[j].x*2, textArea[j].y*2, textArea[j].width*2, textArea[j].height*2, 0,0, (textArea[j].width*2)+10, (textArea[j].height*2)+10);
                                                        resultCanvas.className = "body-img";
                                                        preview.append(resultCanvas);
                                                        preview.append(header);
                                                    })

                                                })
                                        })
                                    }
                                    fileReader.readAsArrayBuffer(file);

                                }

                                preview.className = 'preview-img';
                                preview.onclick = (e) => {
                                    e.stopPropagation()
                                    let figureWin = window.open("", "image_popup", "width=" + e.target.width + ", height=" + (Number(e.target.height) + 120));
                                    figureWin.document.write("<html><body style='margin:0'>")
                                    figureWin.document.write("<small style='font-size: 25px'>header : "+ header.innerText + "</small><br/>")
                                    figureWin.document.write("<a href=javascript:window.close()><img src='" + e.target.toDataURL('image/png') + "' border=0></a><br/>");
                                    figureWin.document.write("<small style='font-size: 25px'>caption : "+ caption.innerText + "</small>")
                                    figureWin.document.write("</body><html>");
                                }
                                img_viewer.append(preview);

                            }

                            pdf_button.innerText = "view pdf";
                            pdf_button.id = "pdf"+i;
                            pdf_button.onclick = (e) => {
                                e.stopPropagation()
                                viewPdf(i);
                            }

                            area.append(pdf_button);
                            area.append(img_viewer);
                            area.append(json_viewer);

                            area.id = "result_area_"+i
                            area.className = "result_areas"

                            e.append(area);
                            $(e).removeClass("li-processing");
                            $(e).addClass("li-done");
                        })


                        let files = document.getElementById('api_file').files;
                        for(let i=0; i<files.length; i++){
                            // getImages(files[i], i);
                        }

                        $('.file-list-li').each((i,e) => {
                            $(e).on("click", function(event) {
                                event.stopPropagation();
                                let display = $("#result_area_"+i).css("display");
                                if(display == 'none'){
                                    $(e).addClass("list-open")
                                    $('#result_area_'+i).show();
                                }else{
                                    $(e).removeClass("list-open")
                                    $('#result_area_'+i).hide();
                                }
                            })
                        })

                        // $('.result_areas').each((i,e) => {
                        //     $(e).off('click');
                        //     $(e).css('cursor', '');
                        // })

                        // let trainXhr = new XMLHttpRequest();
                        // let trainUrl = "/api/trainTempPdf"
                        // trainXhr.open("GET", trainUrl, true);
                        // trainXhr.send();
                    } else {
                        //AjaxError2("Response " + xhr.status + ": " + xhr.responseText);
                        AjaxError2("Response " + xhr.status + ": " );
                    }
                }
            };
            xhr.send(formData);
        }else{
            alert('no file!');
        }
    }

    function getPageImage(fileIndex ,page){
        let file = document.getElementById("api_file").files[fileIndex];
        let fileReader = new FileReader();
        fileReader.onloadend = function(ev) {
            console.log(ev);
            PDFJS.getDocument(fileReader.result).then(function getPdfHelloWorld(pdf) {
                //
                // Fetch the first page
                //
                pdf.getPage(page).then(function getPageHelloWorld(page) {
                    var scale = 1.5;
                    var viewport = page.getViewport(scale);

                    //
                    // Prepare canvas using PDF page dimensions
                    //
                    var canvas = document.getElementById('the-canvas');
                    var context = canvas.getContext('2d');
                    canvas.height = viewport.height;
                    canvas.width = viewport.width;

                    //
                    // Render PDF page into canvas context
                    //
                    var task = page.render({canvasContext: context, viewport: viewport})
                    task.promise.then(function(){
                        let pageImage = new Image();
                        pageImage.src = canvas.toDataURL('image/jpeg');

                    });
                });
            }, function(error){
                console.log(error);
            });
        };
        fileReader.readAsArrayBuffer(file);
    }

    function getImage(path, index){
        let imageXhr = new XMLHttpRequest();
        let imgUrl = "/api/getImage";
        let form = new FormData();
        form.append("path", path);
        imageXhr.responseType = "arraybuffer";
        imageXhr.open('POST', imgUrl, true);
        imageXhr.onreadystatechange = function() {
            if(imageXhr.readyState == 4) {
                if (imageXhr.status == 200) {
                    let img = document.createElement('img');
                    img.src = window.URL.createObjectURL(new Blob([imageXhr.response]));
                    img.className = ("body-img");
                    $("#preview-box-"+index).prepend(img);
                }
            }
        }
        imageXhr.send(form);
    }

    function getTableImage(areaArray, pdfFile, index){
        let tableXhr = new XMLHttpRequest();
        let tableUrl = "/api/getTableImage";
        let form = new FormData();
        // for (let key in areaArray) {
        //     form.append(key, areaArray[key])
        // }
        form.append("areaArray", areaArray)
        form.append("input", pdfFile);
        tableXhr.responseType = "arraybuffer";
        tableXhr.open("POST", tableUrl, true);
        tableXhr.onreadystatechange = function() {
            if(tableXhr.readyState == 4) {
                if (tableXhr.status == 200) {
                    let img = document.createElement('img');
                    img.src = window.URL.createObjectURL(new Blob([tableXhr.response]));
                    img.className = ("body-img");
                    $("#preview-table-box-"+index).prepend(img);
                }
            }
        }
        tableXhr.send(form);

    }

    function viewPdf(i){
        const pdfViewer = document.getElementById('pdfViewer');
        if(pdfViewer.style.display == 'none'){
            $(pdfViewer).show();
        }
        document.getElementsByClassName('container_')[0].style.width="1800px";
        let reader = new FileReader();
        reader.onloadend = function() {
            let pdfAsArray = new Uint8Array(reader.result);
            pdfViewer.contentWindow.PDFViewerApplication.open(pdfAsArray);
            // let xhr = new XMLHttpRequest()
            // let url = '/api/getRange';
            // let form = new FormData();
            // form.append("input", document.getElementById("api_file").files[i]);
            // xhr.responseType = 'json';
            // xhr.open("POST", url, true);
            // xhr.onreadystatechange = function(e) {
            //     if(xhr.readyState == 4){
            //         if (xhr.status == 200) {
            //             let res = e.target.response;
            //
            //            //  let pdfV = pdfViewer.contentWindow.PDFViewerApplication.pdfViewer;
            //            //  console.log(pdfV);
            //            //  let loadingTask = pdfViewer.contentWindow.PDFViewerApplication.pdfViewer.getPageView(3);
            //            // console.log(loadingTask)
            //
            //             const pageSize = Object.keys(res).length - 1;
            //             const pwidth = res[0][0].x1;
            //             const pheight = res[0][0].y1;
            //             let canvas;
            //             let document = pdfViewer.contentWindow.document;
            //             for(let z=1; z<pageSize; z++){
            //                 let pageView = pdfViewer.contentWindow.PDFViewerApplication.pdfViewer.getPageView(z);
            //                 if (pageView.renderingState == 0) {
            //                     pageView.renderingQueue.renderView(pageView);
            //                     // pageView.draw()
            //                     //     .then(() => {
            //                             canvas = document.getElementById('page'+z);
            //                             let ctx = canvas.getContext('2d');
            //                             ctx.globalAlpha = 0.2;
            //                             let cwidth = canvas.width;
            //                             let cheight = canvas.height;
            //                             let w = cwidth / pwidth;
            //                             let h = cheight / pheight;
            //                             let rangeArr = res[z];
            //                             for(let j=0; j<rangeArr.length; j++){
            //                                 let p = rangeArr[j];
            //                                 ctx.fillRect(p.x1 * w, p.y1 * h, (p.x2 - p.x1) * w, (p.y2 - p.y1) * h);
            //                             }
            //                         // })
            //                 }else{
            //                     canvas = document.getElementById('page'+z);
            //                     let ctx = canvas.getContext('2d');
            //                     ctx.globalAlpha = 0.2;
            //                     let cwidth = canvas.width;
            //                     let cheight = canvas.height;
            //                     let w = cwidth / pwidth;
            //                     let h = cheight / pheight;
            //                     let rangeArr = res[z];
            //                     for(let j=0; j<rangeArr.length; j++){
            //                         let p = rangeArr[j];
            //                         ctx.fillRect(p.x1 * w, p.y1 * h, (p.x2 - p.x1) * w, (p.y2 - p.y1) * h);
            //                     }
            //                 }
            //
            //
            //             }
            //         }
            //     }
            // }
            // xhr.send(form);

        }
        reader.readAsArrayBuffer(document.getElementById("api_file").files[i]);

    }

    function getImages(file, i){
        let form = new FormData();
        form.append("input", file);
        let imageXhr = new XMLHttpRequest();
        let imgUrl = "/api/getBodyImages";
        imageXhr.responseType = "arraybuffer";
        imageXhr.open('POST', imgUrl, true);
        imageXhr.onreadystatechange = function() {
            if(imageXhr.readyState == 4){
                if(imageXhr.status == 200){
                    let jszip = new JSZip();
                    jszip.loadAsync(imageXhr.response)
                        .then(function (zip) {
                            zip.forEach(function(fileName){
                                let file = zip.file(fileName);
                                file.async("blob").then(
                                    function success(buf){
                                        let img = document.createElement('img');
                                        img.src = window.URL.createObjectURL(buf);
                                        img.className = ("body-img");
                                        img.id = fileName;
                                        img.style.order = fileName.split("-")[1].split(".")[0]
                                        img.onmouseenter = (e) => {
                                            let originImageArea = document.createElement("div");
                                            let text = document.createElement("span");
                                            text.innerText = "figure"
                                            let originImg = document.createElement("img");
                                            originImg.src = window.URL.createObjectURL(buf);
                                            originImageArea.appendChild(originImg);
                                            originImageArea.appendChild(document.createElement("br"));
                                            originImageArea.appendChild(text);
                                            originImageArea.style.left = e.clientX
                                            originImageArea.style.top = e.target.scrollTop + e.target.scrollHeight;
                                            originImageArea.className = "hover-image"
                                            $('#img_viewer_'+i).append(originImageArea);
                                        }
                                        img.onmouseleave = (e) => {
                                            $('#img_viewer_'+i).find('.hover-image').remove();
                                        }
                                        $('#img_viewer_'+i).append(img);
                                    }
                                )
                            })
                        $('#img_viewer_'+i).find(".loading").remove();
                        })
                        .catch((err) =>{})
                }else if(imageXhr.status == 204){
                    document.getElementById("img_viewer_"+i).innerText = "no content"
                }
            }

        }
        imageXhr.send(form)
    }

	function setupAnnotations(response) {
		// we must check/wait that the corresponding PDF page is rendered at this point
		$('#infoResult2').html('');
		var json = response;
		var pageInfo = json.pages;

		var page_height = 0.0;
		var page_width = 0.0;

        // formulas
        var formulas = json.formulas;
        var mapFormulas = {};
        if (formulas) {
            for(var n in formulas) {
                var annotation = formulas[n];
                var theId = annotation.id;
                var pos = annotation.pos;
                if (pos)
                    mapFormulas[theId] = annotation;
                //for (var m in pos) {
                pos.forEach(function(thePos, m) {
                    //var thePos = pos[m];
                    // get page information for the annotation
                    var pageNumber = thePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateFormula(true, theId, thePos, null, page_height, page_width, null);
                });
            }
        }

        var formulaMarkers = json.formulaMarkers;
		if (formulaMarkers) {
			formulaMarkers.forEach(function(annotation, n) {
				var theId = annotation.id;
				//if (!theId)
                //    return;
				// we take the first and last positions
				var targetFormula = null;
				if (theId)
					targetFormula = mapFormulas[theId];
				if (targetFormula) {
					var theFormulaPos = {};
					var pos = targetFormula.pos;
					if (pos.length == 1) 
						theFormulaPos = pos[0]
					else {
						//if (pos && (pos.length > 0)) {
						var theFirstPos = pos[0];
						var theLastPos = pos[pos.length-1];
						theFormulaPos.p = theFirstPos.p;
						theFormulaPos.w = Math.max(theFirstPos.w, theLastPos.w);
						theFormulaPos.h = Math.max(Math.abs(theLastPos.y - theFirstPos.y), theFirstPos.h) + Math.max(theFirstPos.h, theLastPos.h);
						theFormulaPos.x = Math.min(theFirstPos.x, theLastPos.x);
						theFormulaPos.y = Math.min(theFirstPos.y, theLastPos.y);
					}
					var pageNumber = theFormulaPos.p;
					if (pageInfo[pageNumber-1]) {
						page_height = pageInfo[pageNumber-1].page_height;
						page_width = pageInfo[pageNumber-1].page_width;
					}
					annotateFormula(false, theId, annotation, null, page_height, page_width, theFormulaPos);
					//}
				} else {
					var pageNumber = annotation.p;
					if (pageInfo[pageNumber-1]) {
						page_height = pageInfo[pageNumber-1].page_height;
						page_width = pageInfo[pageNumber-1].page_width;
					}
					annotateFormula(false, theId, annotation, null, page_height, page_width, null);
				}
			});
		}

        // figures
        var figures = json.figures;
        var mapFigures = {};
        if (figures) {
            for(var n in figures) {
                var annotation = figures[n];
                var theId = annotation.id;
                var pos = annotation.pos;
                if (pos)
                    mapFigures[theId] = annotation;
                pos.forEach(function(thePos, m) {
                    // get page information for the annotation
                    var pageNumber = thePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateFigure(true, theId, thePos, null, page_height, page_width, null);
                });
            }
        }

        var figureMarkers = json.figureMarkers;
        if (figureMarkers) {
            figureMarkers.forEach(function(annotation, n) {
                var theId = annotation.id;
                // we take the first and last positions
                var targetFigure = null;
                if (theId)
                    targetFigure = mapFigures[theId];
                if (targetFigure) {
                    var theFigurePos = {};
                    var pos = targetFigure.pos;
                    if (pos.length == 1) 
                        theFigurePos = pos[0];
                    else {
                        // for figure we have to scan all the component positions, because graphic objects are not sorted
                        var theFirstPos = pos[0];
                        theFigurePos.p = theFirstPos.p;
                        theFigurePos.x = theFirstPos.x;
                        theFigurePos.y = theFirstPos.y;
                        theFigurePos.h = theFirstPos.h;
                        theFigurePos.w = theFirstPos.w;

                        for (thePosIndex in pos) {
                            if (thePosIndex == 0)
                                continue
                            thePos = pos[thePosIndex]
                            if (thePos.x < theFigurePos.x) {
                                theFigurePos.w = theFigurePos.w + (theFigurePos.x - thePos.x);
                                theFigurePos.x = thePos.x;
                            }
                            if (thePos.y < theFigurePos.y) {
                                theFigurePos.h = theFigurePos.h + (theFigurePos.y - thePos.y);
                                theFigurePos.y = thePos.y;
                            }

                            var maxFigureX = theFigurePos.x + theFigurePos.w;
                            var maxFigureY = theFigurePos.y + theFigurePos.h;

                            var maxPosX = thePos.x + thePos.w;
                            var maxPosY = thePos.y + thePos.h;

                            if (maxPosX > maxFigureX) {
                                theFigurePos.w = maxPosX - theFigurePos.x;
                            }

                            if (maxPosY > maxFigureY) {
                                theFigurePos.h = maxPosY - theFigurePos.y;
                            }
                        }
                    }
                    var pageNumber = theFigurePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateFigure(false, theId, annotation, null, page_height, page_width, theFigurePos);
                } else {
                    var pageNumber = annotation.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateFigure(false, theId, annotation, null, page_height, page_width, null);
                }
            });
        }

        // tables
        var tables = json.tables;
        var mapTables = {};
        if (tables) {
            for(var n in tables) {
                var annotation = tables[n];
                var theId = annotation.id;
                var pos = annotation.pos;
                if (pos)
                    mapTables[theId] = annotation;
                pos.forEach(function(thePos, m) {
                    // get page information for the annotation
                    var pageNumber = thePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateTable(true, theId, thePos, null, page_height, page_width, null);
                });
            }
        }

        var tableMarkers = json.tableMarkers;
        if (tableMarkers) {
            tableMarkers.forEach(function(annotation, n) {
                var theId = annotation.id;
                //if (!theId)
                //    return;
                // we take the first and last positions
                var targetTable = null;
                if (theId)
                    targetTable = mapTables[theId];
                if (targetTable) {
                    var theTablePos = {};
                    var pos = targetTable.pos;
                    if (pos.length == 1) 
                        theTablePos = pos[0]
                    else {
                        //if (pos && (pos.length > 0)) {
                        var theFirstPos = pos[0];
                        var theLastPos = pos[pos.length-1];
                        theTablePos.p = theFirstPos.p;
                        theTablePos.w = Math.max(theFirstPos.w, theLastPos.w);
                        theTablePos.h = Math.max(Math.abs(theLastPos.y - theFirstPos.y), theFirstPos.h) + Math.max(theFirstPos.h, theLastPos.h);
                        theTablePos.x = Math.min(theFirstPos.x, theLastPos.x);
                        theTablePos.y = Math.min(theFirstPos.y, theLastPos.y);
                    }
                    var pageNumber = theTablePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateTable(false, theId, annotation, null, page_height, page_width, theTablePos);
                    //}
                } else {
                    var pageNumber = annotation.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateTable(false, theId, annotation, null, page_height, page_width, null);
                }
            });
        }

        var refBibs = json.refBibs;
        var mapRefBibs = {};
        if (refBibs) {
            for(var n in refBibs) {
                var annotation = refBibs[n];
                var theId = annotation.id;
                var theUrl = annotation.url;
                var pos = annotation.pos;
                if (pos)
                    mapRefBibs[theId] = annotation;
                //for (var m in pos) {
                pos.forEach(function(thePos, m) {
                    //var thePos = pos[m];
                    // get page information for the annotation
                    var pageNumber = thePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateBib(true, theId, thePos, theUrl, page_height, page_width, null);
                });
            }
        }

        // we need the above mapRefBibs structure to be created to perform the ref. markers analysis
        var refMarkers = json.refMarkers;
        if (refMarkers) {
            //for(var n in refMarkers) {
            refMarkers.forEach(function(annotation, n) {
                //var annotation = refMarkers[n];
                var theId = annotation.id;
                //if (!theId)
                //    return;
                // we take the first and last positions
                var targetBib = mapRefBibs[theId];
                if (targetBib) {
                    var theBibPos = {};
                    var pos = targetBib.pos;
                    if (pos.length == 1) 
                        theBibPos = pos[0];
                    else {
                        //if (pos && (pos.length > 0)) {
                        var theFirstPos = pos[0];
                        // we can't visualize over two pages, so we take as theLastPos the last coordinate position on the page of theFirstPos
                        
                        var theLastPos = pos[pos.length-1];
                        if (theLastPos.p != theFirstPos.p) {
                            var k = 2;
                            while (pos.length-k>0) {
                                theLastPos = pos[pos.length-k];
                                if (theLastPos.p == theFirstPos.p) 
                                    break;
                                k++;
                            }
                        }
                        theBibPos.p = theFirstPos.p;
                        theBibPos.w = Math.max(theFirstPos.w, theLastPos.w);
                        theBibPos.h = Math.max(Math.abs(theLastPos.y - theFirstPos.y), theFirstPos.h) + Math.max(theFirstPos.h, theLastPos.h);
                        theBibPos.x = Math.min(theFirstPos.x, theLastPos.x);
                        theBibPos.y = Math.min(theFirstPos.y, theLastPos.y);
                    }
                    var pageNumber = theBibPos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateBib(false, theId, annotation, null, page_height, page_width, theBibPos);
                    //}
                } else {
                    var pageNumber = annotation.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateBib(false, theId, annotation, null, page_height, page_width, null);
                }
            });
        }
	}

	function annotateBib(bib, theId, thePos, url, page_height, page_width, theBibPos) {
		var page = thePos.p;
		var pageDiv = $('#page-'+page);
		var canvas = pageDiv.children('canvas').eq(0);;

		var canvasHeight = canvas.height();
		var canvasWidth = canvas.width();
		var scale_x = canvasHeight / page_height;
		var scale_y = canvasWidth / page_width;

		var x = thePos.x * scale_x;
		var y = thePos.y * scale_y;
		var width = thePos.w * scale_x;
		var height = thePos.h * scale_y;

//console.log('annotate: ' + page + " " + x + " " + y + " " + width + " " + height);
//console.log('location: ' + canvasHeight + " " + canvasWidth);
//console.log('location: ' + page_height + " " + page_width);
		//make clickable the area
		var element = document.createElement("a");
		var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+y+"px; left:"+x+"px;";

		if (bib) {
			// this is a bibliographical reference
			// we draw a line
			if (url) {
				element.setAttribute("style", attributes + "border:2px; border-style:none none solid none; border-color: blue;");
				element.setAttribute("href", url);
				element.setAttribute("target", "_blank");
			}
			else
				element.setAttribute("style", attributes + "border:1px; border-style:none none dotted none; border-color: gray;");
			element.setAttribute("id", theId);
		} else {
			// this is a reference marker
			// we draw a box (blue if associated to an id, gray otherwise)			
			if (theId) {
                element.setAttribute("style", attributes + "border:1px solid; border-color: blue;");
                // the link here goes to the bibliographical reference
				element.onclick = function() {
					goToByScroll(theId);
				};
			} else
                element.setAttribute("style", attributes + "border:1px solid; border-color: gray;");

			// we need the area where the actual target bibliographical reference is
			if (theBibPos) {
				element.setAttribute("data-toggle", "popover");
				element.setAttribute("data-placement", "top");
				element.setAttribute("data-content", "content");
				element.setAttribute("data-trigger", "hover");
				var newWidth = theBibPos.w * scale_x;
				var newHeight = theBibPos.h * scale_y;
				var newImg = getImagePortion(theBibPos.p, newWidth, newHeight, theBibPos.x * scale_x, theBibPos.y * scale_y);
				$(element).popover({
					content:  function () {
						return '<img src=\"'+ newImg + '\" style=\"width:100%\" />';
						//return '<img src=\"'+ newImg + '\" />';
					},
					html: true,
					container: 'body'
					//width: newWidth + 'px',
					//height: newHeight + 'px'
//					container: canvas,
					//width: '600px',
					//height: '100px'
    			});
			}
		}
		pageDiv.append(element);
	}

    function annotateFormula(formula, theId, thePos, url, page_height, page_width, theFormulaPos) {
        var page = thePos.p;
        var pageDiv = $('#page-'+page);
        var canvas = pageDiv.children('canvas').eq(0);;

        var canvasHeight = canvas.height();
        var canvasWidth = canvas.width();
        var scale_x = canvasHeight / page_height;
        var scale_y = canvasWidth / page_width;

        var x = thePos.x * scale_x;
        var y = thePos.y * scale_y;
        var width = thePos.w * scale_x;
        var height = thePos.h * scale_y;

//console.log('annotate: ' + page + " " + x + " " + y + " " + width + " " + height);
//console.log('location: ' + canvasHeight + " " + canvasWidth);
//console.log('location: ' + page_height + " " + page_width);
        //make clickable the area
        var element = document.createElement("a");
        var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+y+"px; left:"+x+"px;";

        if (formula) {
            // this is a formula
            // we draw a line
            element.setAttribute("style", attributes + "border:1px; border-style:dotted; border-color: red;");
            element.setAttribute("title", "formula");
            element.setAttribute("id", theId);
        } else {
            // this is a formula reference marker    
            // we draw a box (red if associated to an id, gray otherwise)
            if (theId) {
                element.setAttribute("style", attributes + "border:1px solid; border-color: red;");
                // the link here goes to the referenced formula
                element.onclick = function() {
                    goToByScroll(theId);
                };
            } else
                 element.setAttribute("style", attributes + "border:1px solid; border-color: gray;");

            // we need the area where the actual target formula is
            if (theFormulaPos) {
                element.setAttribute("data-toggle", "popover");
                element.setAttribute("data-placement", "top");
                element.setAttribute("data-content", "content");
                element.setAttribute("data-trigger", "hover");
                var newWidth = theFormulaPos.w * scale_x;
                var newHeight = theFormulaPos.h * scale_y;
                var newImg = getImagePortion(theFormulaPos.p, newWidth, newHeight, theFormulaPos.x * scale_x, theFormulaPos.y * scale_y);
                $(element).popover({
                    content:  function () {
                        return '<img src=\"'+ newImg + '\" style=\"width:100%\" />';
                        //return '<img src=\"'+ newImg + '\" />';
                    },
                    html: true,
                    container: 'body'
                    //width: newWidth + 'px',
                    //height: newHeight + 'px'
//                  container: canvas,
                    //width: '600px',
                    //height: '100px'
                });
            }
        }
        pageDiv.append(element);
    }

    function annotateFigure(figure, theId, thePos, url, page_height, page_width, theFigurePos) {
        var page = thePos.p;
        var pageDiv = $('#page-'+page);
        var canvas = pageDiv.children('canvas').eq(0);;

        var canvasHeight = canvas.height();
        var canvasWidth = canvas.width();
        var scale_x = canvasHeight / page_height;
        var scale_y = canvasWidth / page_width;

        var x = thePos.x * scale_x;
        var y = thePos.y * scale_y;
        var width = thePos.w * scale_x;
        var height = thePos.h * scale_y;

//console.log('annotate: ' + page + " " + x + " " + y + " " + width + " " + height);
//console.log('location: ' + canvasHeight + " " + canvasWidth);
//console.log('location: ' + page_height + " " + page_width);
        //make clickable the area
        var element = document.createElement("a");
        var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+y+"px; left:"+x+"px;";

        if (figure) {
            // this is a figure
            // we draw a line
            element.setAttribute("style", attributes + "border:1px; border-style:dotted; border-color: blue;");
            element.setAttribute("title", "figure");
            element.setAttribute("id", theId);
        } else {
            // this is a figure reference marker    
            // we draw a box (blue if associated to an id, gray otherwise)
            if (theId) {
                element.setAttribute("style", attributes + "border:1px solid; border-color: blue;");
                // the link here goes to the referenced figure
                element.onclick = function() {
                    goToByScroll(theId);
                };
            } else
                 element.setAttribute("style", attributes + "border:1px solid; border-color: gray;");

            // we need the area where the actual target figure is
            if (theFigurePos) {
                element.setAttribute("data-toggle", "popover");
                element.setAttribute("data-placement", "top");
                element.setAttribute("data-content", "content");
                element.setAttribute("data-trigger", "hover");
                var newWidth = theFigurePos.w * scale_x;
                var newHeight = theFigurePos.h * scale_y;
                var newImg = getImagePortion(theFigurePos.p, newWidth, newHeight, theFigurePos.x * scale_x, theFigurePos.y * scale_y);
                $(element).popover({
                    content:  function () {
                        return '<img src=\"'+ newImg + '\" style=\"width:100%\" />';
                        //return '<img src=\"'+ newImg + '\" />';
                    },
                    html: true,
                    container: 'body'
                    //width: newWidth + 'px',
                    //height: newHeight + 'px'
//                  container: canvas,
                    //width: '600px',
                    //height: '100px'
                });
            }
        }
        pageDiv.append(element);
    }

    function annotateTable(table, theId, thePos, url, page_height, page_width, theTablePos) {
        var page = thePos.p;
        var pageDiv = $('#page-'+page);
        var canvas = pageDiv.children('canvas').eq(0);;

        var canvasHeight = canvas.height();
        var canvasWidth = canvas.width();
        var scale_x = canvasHeight / page_height;
        var scale_y = canvasWidth / page_width;

        var x = thePos.x * scale_x;
        var y = thePos.y * scale_y;
        var width = thePos.w * scale_x;
        var height = thePos.h * scale_y;

//console.log('annotate: ' + page + " " + x + " " + y + " " + width + " " + height);
//console.log('location: ' + canvasHeight + " " + canvasWidth);
//console.log('location: ' + page_height + " " + page_width);
        //make clickable the area
        var element = document.createElement("a");
        var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+y+"px; left:"+x+"px;";

        if (table) {
            // this is a table
            // we draw a line
            element.setAttribute("style", attributes + "border:1px; border-style:dotted; border-color: blue;");
            element.setAttribute("title", "table");
            element.setAttribute("id", theId);
        } else {
            // this is a table reference marker    
            // we draw a box (blue if associated to an id, gray otherwise)
            if (theId) {
                element.setAttribute("style", attributes + "border:1px solid; border-color: blue;");
                // the link here goes to the referenced table
                element.onclick = function() {
                    goToByScroll(theId);
                };
            } else
                 element.setAttribute("style", attributes + "border:1px solid; border-color: gray;");

            // we need the area where the actual target table is
            if (theTablePos) {
                element.setAttribute("data-toggle", "popover");
                element.setAttribute("data-placement", "top");
                element.setAttribute("data-content", "content");
                element.setAttribute("data-trigger", "hover");
                var newWidth = theTablePos.w * scale_x;
                var newHeight = theTablePos.h * scale_y;
                var newImg = getImagePortion(theTablePos.p, newWidth, newHeight, theTablePos.x * scale_x, theTablePos.y * scale_y);
                $(element).popover({
                    content:  function () {
                        return '<img src=\"'+ newImg + '\" style=\"width:100%\" />';
                        //return '<img src=\"'+ newImg + '\" />';
                    },
                    html: true,
                    container: 'body'
                    //width: newWidth + 'px',
                    //height: newHeight + 'px'
//                  container: canvas,
                    //width: '600px',
                    //height: '100px'
                });
            }
        }
        pageDiv.append(element);
    }


	/* jquery-based movement to an anchor, without modifying the displayed url and a bit smoother */
	function goToByScroll(id) {
    	$('html,body').animate({scrollTop: $("#"+id).offset().top},'fast');
	}

	/* croping an area from a canvas */
	function getImagePortion(page, width, height, x, y) {
//console.log("page: " + page + ", width: " + width + ", height: " + height + ", x: " + x + ", y: " + y);
		// get the page div
		var pageDiv = $('#page-'+page);
//console.log(page);
		// get the source canvas
		var canvas = pageDiv.children('canvas')[0];
		// the destination canvas
		var tnCanvas = document.createElement('canvas');
 		var tnCanvasContext = tnCanvas.getContext('2d');
 		tnCanvas.width = width;
    	tnCanvas.height = height;
		tnCanvasContext.drawImage(canvas, x , y, width, height, 0, 0, width, height);
 		return tnCanvas.toDataURL();
	}

	function SubmitSuccesful3(responseText, statusText, xhr) {
		var display = "<pre class='prettyprint lang-xml' id='xmlCode'>";
		var testStr = vkbeautify.xml(responseText);
        teiPatentToDownload = responseText;
		display += htmll(testStr);
		display += "</pre>";
		$('#requestResult3').html(display);
		window.prettyPrint && prettyPrint();
		$('#requestResult3').show();
        $("#btn_download3").show();
	}

	function setupPatentAnnotations(response) {
		// we must check/wait that the corresponding PDF page is rendered at this point

		var json = response;
		var pageInfo = json.pages;

		var page_height = 0.0;
		var page_width = 0.0;

		var patents = json.patents;
		if (patents) {
			for(var n in patents) {
				var annotation = patents[n];
				var pos = annotation.pos;
				var theUrl = null;
				if (annotation.url && annotation.url.espacenet)
					theUrl = annotation.url.espacenet;
				else if (annotation.url && annotation.url.epoline)
					theUrl = annotation.url.epoline;
				pos.forEach(function(thePos, m) {
					// get page information for the annotation
					var pageNumber = thePos.p;
					if (pageInfo[pageNumber-1]) {
						page_height = pageInfo[pageNumber-1].page_height;
						page_width = pageInfo[pageNumber-1].page_width;
					}
					annotatePatentBib(true, thePos, theUrl, page_height, page_width);
				});
			}
		}

		var refBibs = json.articles;
		if (refBibs) {
			for(var n in refBibs) {
				var annotation = refBibs[n];
				//var theId = annotation.id;
				var theUrl = null;
				var pos = annotation.pos;
				//if (pos)
				//	mapRefBibs[theId] = annotation;
				//for (var m in pos) {
				pos.forEach(function(thePos, m) {
					//var thePos = pos[m];
					// get page information for the annotation
					var pageNumber = thePos.p;
					if (pageInfo[pageNumber-1]) {
						page_height = pageInfo[pageNumber-1].page_height;
						page_width = pageInfo[pageNumber-1].page_width;
					}
					annotatePatentBib(false, thePos, theUrl, page_height);
				});
			}
		}
	}

	function annotatePatentBib(isPatent, thePos, url, page_height, page_width, theBibPos) {
		var page = thePos.p;
		var pageDiv = $('#page-'+page);
		var canvas = pageDiv.children('canvas').eq(0);

		var canvasHeight = canvas.height();
		var canvasWidth = canvas.width();
		var scale_y = canvasHeight / page_height;
		var scale_x = canvasWidth / page_width;

		var x = thePos.x * scale_x;
		var y = thePos.y * scale_y;
		var width = thePos.w * scale_x;
		var height = thePos.h * scale_y;

//console.log('annotate: ' + page + " " + x + " " + y + " " + width + " " + height);
//console.log('location: ' + canvasHeight + " " + canvasWidth);
//console.log('location: ' + page_height + " " + page_width);
		//make clickable the area
		var element = document.createElement("a");
		var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+y+"px; left:"+x+"px;";

		if (patent) {
			// this is a patent reference
			// we draw a line
			if (url) {
				element.setAttribute("style", attributes + "border:2px; border-style:none none solid none; border-color: blue;");
				element.setAttribute("href", url);
				element.setAttribute("target", "_blank");
			}
			else
				element.setAttribute("style", attributes + "border:1px; border-style:none none dotted none; border-color: gray;");
		} else {
			// this is a NPL bibliographical reference
			// we draw a box
			element.setAttribute("style", attributes + "border:1px solid; border-color: blue;");

			/*element.setAttribute("data-toggle", "popover");
			element.setAttribute("data-placement", "top");
			element.setAttribute("data-content", "content");
			element.setAttribute("data-trigger", "hover");

			$(element).popover({
				content:  'content',
				html: true,
				container: 'body'
				//width: newWidth + 'px',
				//height: newHeight + 'px'
//					container: canvas,
				//width: '600px',
				//height: '100px'
			});*/

		}
		pageDiv.append(element);
	}

	$(document).ready(function() {
	    $(document).on('shown', '#xmlCode', function(event) {
	        prettyPrint();
	    });
	});

	function processChange() {
		var selected = $('#selectedService option:selected').attr('value');
		if (block == 1)
			selected = $('#selectedService2 option:selected').attr('value');
		else if (block == 2)
			selected = $('#selectedService3 option:selected').attr('value');

		if (selected == 'processHeaderDocument') {
			createInputFile(selected);
			$('#consolidateBlock1').show();
			$('#consolidateBlock2').hide();
			$('#includeRawAffiliationsBlock').show();
			$('#includeRawCitationsBlock').hide();
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processHeaderDocument');
		}
		else if (selected == 'processFulltextDocument') {
			createInputFile(selected);
			$('#consolidateBlock1').show();
			$('#consolidateBlock2').show();
			$('#includeRawAffiliationsBlock').show();
			$('#includeRawCitationsBlock').show();
            $('#segmentSentencesBlock').show();
            $('#teiCoordinatesBlock').show();
			setBaseUrl('processFulltextDocument');
		}
		else if (selected == 'processDate') {
			createInputTextArea('date');
			$('#consolidateBlock1').hide();
			$('#consolidateBlock2').hide();
			$('#includeRawAffiliationsBlock').hide();
			$('#includeRawCitationsBlock').hide();
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processDate');
		}
		else if (selected == 'processHeaderNames') {
			createInputTextArea('names');
			$('#consolidateBlock1').hide();
			$('#consolidateBlock2').hide();
			$('#includeRawAffiliationsBlock').hide();
			$('#includeRawCitationsBlock').hide();
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processHeaderNames');
		}
		else if (selected == 'processCitationNames') {
			createInputTextArea('names');
			$('#consolidateBlock1').hide();
			$('#consolidateBlock2').hide();
			$('#includeRawAffiliationsBlock').hide();
			$('#includeRawCitationsBlock').hide();
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processCitationNames');
		}
		else if (selected == 'processReferences') {
			createInputFile(selected);
			$('#consolidateBlock1').hide();
			$('#consolidateBlock2').show();
			$('#includeRawAffiliationsBlock').hide();
			$('#includeRawCitationsBlock').show();
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processReferences');
		}
		else if (selected == 'processAffiliations') {
			createInputTextArea('affiliations');
			$('#consolidateBlock1').hide();
			$('#consolidateBlock2').hide();
			$('#includeRawAffiliationsBlock').hide();
			$('#includeRawCitationsBlock').hide();
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processAffiliations');
		}
		else if (selected == 'processCitation') {
			createInputTextArea('citations');
			$('#consolidateBlock1').hide();
			$('#consolidateBlock2').show();
			$('#includeRawAffiliationsBlock').hide();
			$('#includeRawCitationsBlock').hide();
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processCitation');
		}
		/*else if (selected == 'processCitationPatentTEI') {
			createInputFile3(selected);
			$('#consolidateBlock3').show();
			setBaseUrl('processCitationPatentTEI');
		}*/
		else if (selected == 'processCitationPatentST36') {
			createInputFile3(selected);
			$('#consolidateBlock3').show();
			setBaseUrl('processCitationPatentST36');
		}
		else if (selected == 'processCitationPatentPDF') {
			createInputFile3(selected);
			$('#consolidateBlock3').show();
			setBaseUrl('processCitationPatentPDF');
		}
		else if (selected == 'processCitationPatentTXT') {
			createInputTextArea3('input');
			$('#consolidateBlock3').show();
			setBaseUrl('processCitationPatentTXT');
		}
		else if (selected == 'referenceAnnotations') {
			createInputFile2(selected);
			$('#consolidateBlockPDFRef').show();
            $('#consolidateBlockPDFFig').show();
			setBaseUrl('referenceAnnotations');
		}
		else if (selected == 'annotatePDF') {
			createInputFile2(selected);
			$('#consolidateBlockPDFRef').show();
            $('#consolidateBlockPDFFig').hide();
			setBaseUrl('annotatePDF');
		}
		else if (selected == 'citationPatentAnnotations') {
			createInputFile3(selected);
			$('#consolidateBlock3').show();
			setBaseUrl('citationPatentAnnotations');
		}
	}

	function createInputFile(selected) {
		//$('#label').html('&nbsp;');
		$('#textInputDiv').hide();
		$('#fileInputDiv').show();

		$('#gbdForm').attr('enctype', 'multipart/form-data');
		$('#gbdForm').attr('method', 'post');
	}

	function createInputFile2(selected) {
		//$('#label').html('&nbsp;');
		$('#textInputDiv2').hide();
		$('#fileInputDiv2').show();

		$('#gbdForm2').attr('enctype', 'multipart/form-data');
		$('#gbdForm2').attr('method', 'post');
	}

	function createInputFile3(selected) {
		//$('#label').html('&nbsp;');
		$('#textInputDiv3').hide();
		$('#fileInputDiv3').show();

		$('#gbdForm3').attr('enctype', 'multipart/form-data');
		$('#gbdForm3').attr('method', 'post');
	}

	function createInputTextArea(nameInput) {
		//$('#label').html('&nbsp;');
		$('#fileInputDiv').hide();
		//$('#input').remove();

		//$('#field').html('<table><tr><td><textarea class="span7" rows="5" id="input" name="'+nameInput+'" /></td>'+
		//"<td><span style='padding-left:20px;'>&nbsp;</span></td></tr></table>");
		$('#textInputArea').attr('name', nameInput);
		$('#textInputDiv').show();

		$('#gbdForm').attr('enctype', '');
		$('#gbdForm').attr('method', 'post');
	}

	function createInputTextArea3(nameInput) {
		//$('#label').html('&nbsp;');
		$('#fileInputDiv3').hide();

		$('#textInputArea3').attr('name', nameInput);
		$('#textInputDiv3').show();

		$('#gbdForm3').attr('enctype', '');
		$('#gbdForm3').attr('method', 'post');
	}

	function download(){
        var name ="export";
		if (document.getElementById("input")
            && document.getElementById("input").files.length > 0
                && document.getElementById("input").files[0].name) {
             name = document.getElementById("input").files[0].name;
        }

		var fileName = name + ".tei.xml";
	    var a = document.createElement("a");

	    var file = new Blob([teiToDownload], {type: 'application/xml'});
	    var fileURL = URL.createObjectURL(file);
	    a.href = fileURL;
	    a.download = fileName;

	    document.body.appendChild(a);

	    $(a).ready(function() {
			a.click();
			return true;
		});


		// old method to download but with well formed xm but not beautified
	    /*var a = document.body.appendChild(
	        document.createElement("a")
	    );
	    a.download = "export.xml";
	    var xmlData = $.parseXML(teiToDownload);

	    if (window.ActiveXObject){
	        var xmlString = xmlData.xml;
	    } else {
	        var xmlString = (new XMLSerializer()).serializeToString(xmlData);
	    }
	    a.href = "data:text/xml," + xmlString; // Grab the HTML
	    a.click(); // Trigger a click on the element*/
	}

	function downloadPatent() {
        var name = "export";
        if (document.getElementById("input3")
            && document.getElementById("input3").files.length > 0
            && document.getElementById("input3").files[0].name) {
            name = document.getElementById("input3").files[0].name;
        }
        var fileName = name + ".tei.xml";
        var a = document.createElement("a");


        var file = new Blob([teiPatentToDownload], {type: 'application/xml'});
        var fileURL = URL.createObjectURL(file);
        a.href = fileURL;
        a.download = fileName;

        document.body.appendChild(a);

        $(a).ready(function () {
            a.click();
            return true;
        });

    }
    })(jQuery);


function downloadVisibilty(){
    $("#btn_download").hide();
}
function downloadVisibilty3(){
    $("#btn_download3").hide();
}