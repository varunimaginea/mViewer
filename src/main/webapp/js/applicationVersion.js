var GetApplicationVersion = ( function() {

    var viewUrl = 'https://github.com/Imaginea/mViewer';

    var downloadUrl = viewUrl+'/downloads';

    function sendJSONPRequest(url, callback) {
        $.ajax({
            url: url,
            success: function(responseData) {
                callback(responseData);
            },
            dataType: 'jsonp'
        });
    }

    function getLatestVersion(version1, version2) {
        var v1 = version1.split('.');
        var v2 = version2.split('.');
        var greaterVersion = version1;
        for (var i = 0; i < v1.length; i++) {
            if (parseInt(v1[i], 10) === parseInt(v2[i], 10)) {
                continue;
            }
            if (parseInt(v1[i], 10) > parseInt(v2[i], 10)) {
                greaterVersion = version1;
                break;
            } else {
                greaterVersion = version2;
                break;
            }
        }
        return greaterVersion;
    }

    function init() {
        sendJSONPRequest("https://api.github.com/repos/Imaginea/mViewer/tags", function(githubData) {
            if (githubData.data[0]) {
                var latestVersion = '0.0.0';
                $.each(githubData.data, function(index, data) {
                    var numericValue = data.name.replace(/\w/, '');
                    latestVersion = getLatestVersion(latestVersion, numericValue);
                });
                $.get('services/login/appVersion', function(response, status, xhr) {
                    if (response != latestVersion) {
                        latestVersion = getLatestVersion(response, latestVersion);
                        if (response != latestVersion) {
                            var messageAnchor = $("<p>").text('A new Version of mViewer is Available. ');
                            var zipAnchor = $("<a>").attr('href', viewUrl).attr('target', '_blank').text(' View. ');
                            var tarAnchor = $("<a>").attr('href', downloadUrl).attr('target', '_blank').text(' Download.');

                            $('#versionMsg').append(messageAnchor, zipAnchor, tarAnchor).css('display', 'block');
                        }
                    }
                }, 'text');
            }
        });
    }

    return {
        initialize: function() {
            init();
        }
    };
})();
