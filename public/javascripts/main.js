(function ($) {
  String.prototype.format = function () {
    var args = arguments;
    return this.replace(/\{\{|\}\}|\{(\d+)\}/g, function (m, n) {
      if (m == "{{") {
        return "{";
      }
      if (m == "}}") {
        return "}";
      }
      return args[n];
    });
  };
  
  var sectionHTML = '<section class="section--center mdl-grid mdl-grid--no-spacing mdl-shadow--2dp"> ' +
    '     <header class="section__play-btn mdl-cell mdl-cell--3-col-desktop mdl-cell--2-col-tablet mdl-cell--4-col-phone mdl-color--teal-100 mdl-color-text--white"> ' +
    '         <i class="material-icons">description</i> ' +
    '     </header> ' +
    '     <div class="mdl-card mdl-cell mdl-cell--9-col-desktop mdl-cell--6-col-tablet mdl-cell--4-col-phone"> ' +
    '         <div class="mdl-card__supporting-text"> ' +
    '             <h4>{0}</h4> Id={1} ' +
    '         </div> ' +
    '     </div> ' +
    '     <button class="mdl-button mdl-js-button mdl-button--raised mdl-button--colored downloadbutton" data-id="{1}"> ' +
    ' 		Download </button> ' +
    '     <button style="margin-top: 40px;" class="mdl-button mdl-js-button mdl-js-button mdl-button--accent deletebutton" data-id="{1}"> ' +
    '		Delete </button>' +
    ' </section>';

  function showCards(csvs) {
    var $overview = $('#overview');
    $overview.empty();
    
    if(Array.isArray(csvs) && csvs.length < 1) {
    	$overview.append('<h4 style="width: 100%; text-align: center;"> There are no CSV Files available. You can upload one! </h4>');
    }
    
    csvs.forEach(function (csv, index) {
      $overview.append(sectionHTML.format(csv.filename, csv.id))
    });
    $overview.append($('<section />'));
    
    $overview.find('button').each(function() {
    	new MaterialButton(this);
    });
    
    $overview.find('ul').each(function() {
    	new MaterialMenu(this);
    });

    // Add event handlers, download links to the buttons
    $overview.find('.downloadbutton').each(function () {
      var id = $(this).attr('data-id');
      $(this).click(function() {
    	  var a = $('<a/>').attr('href', '/csv/' + id + '/content').attr('target', '_blank');
    	  a.click();
      }); 
    });

    $overview.find('.deletebutton').each(function () {
      $(this).click(function () {
        var id = $(this).attr('data-id');
        deleteCSV(id);
      });
    });
  }

  function getAllCSVs(callback) {
    $.getJSON('/csv', function (data) {
      console.log(data);
      callback(data)
    })
  }

  function updateOverview() {
    getAllCSVs(showCards)
  }

  function showSnackbar(message) {
    var data = {
      message: message
    };
    document.querySelector('#the-toast').MaterialSnackbar.showSnackbar(data);
  }

  function deleteCSV(id) {
    $.ajax({
      type: 'DELETE',
      url: '/csv/' + id,
      headers: {
    	  'CSRF-Bypass': 'dothebypass'
      },
      complete: function (xhr ) {
        var message;
        if (xhr.status === 200) {
          message = 'Delete Successful';
        } else {
          message = 'An error occured: ' + xhr.status;
        }
        showSnackbar(message);
        updateOverview();
      }
    })
  };

  function prepareUpload() {
	var input = $('#the-file-input')[0];
	if(!input) {
      showSnackBar('An error occured. File input not available');
      return;
	}
	if(input.files.length < 1) {
	  showSnackBar('An error occured: no file available.');
	  return;
	}
	
    var file = $('#the-file-input')[0].files[0];

    var $uploadForm = $('#uploadForm').clone();
    $('<input />')
      .attr('type', 'hidden')
      .attr('name', 'filename')
      .attr('value', file.name)
      .appendTo($uploadForm);

      $.ajax({
    	url: '/csv',
        type: 'POST',
        headers: {
      	  'CSRF-Bypass': 'dothebypass'
        },
        data: $uploadForm.serialize(),
        success: function (data) {
          showSnackbar('Transmitting Data');
          uploadFileWithId(file, data.id)
        },
        error: function () {
          showSnackbar('Upload unsuccessful')
        }
    });
  }

  function uploadFileWithId(file, id) {
    $.ajax({
      url: '/csv/' + id + '/content',
      type: 'POST',
      headers: {
    	  'CSRF-Bypass': 'dothebypass'
      },
      data: file,
      processData: false,
      success: function() {
        showSnackbar('Transmission complete.');
        $('#overviewAnchor').click();
      },
      error: function() {
        showSnackbar('Could not transmit data.');
      }
    });
  }
  
  $('#add').click(function() {
	  $('#uploadAnchor').click();
	  setTimeout(function() {
		  $('#the-file-input').click();
	  }, 300);
  })

  $('#uploadSubmitButton').click(function () {
    prepareUpload();
  });

  $('#overviewAnchor').click(function () {
    updateOverview();
  });

  updateOverview();
})(Zepto)
