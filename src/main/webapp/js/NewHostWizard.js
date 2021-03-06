var NewHostWizard = function($scope, $uibModalInstance, $http, $timeout, appsession, uuid4, $log) {
    $scope.pubkeyUptoDate = false;
    $scope.pubkeyUpdating = false;
    $scope.inprg = false;

    $scope.errors = [];

    $scope.host = {
        "@type" : 'host',
        id : uuid4.generate(),
        address : '',
        publicKey : '',
        dedicated : true,
    };

    $scope.powerManagment = [];

    $scope.password = {
    	password: ''
    };

	$scope.usepubkey = true;

	$scope.controllerKey = '';

    $scope.updateTimeout = null;
    $scope.clearPublicKey = function (event) {
		if(event.keyCode == 13) {
			$scope.addHost();
		}
        $scope.host.publicKey = '';
    }
    $scope.errorHandler = function(error, responseCode) {
		$scope.errors.push(error);
    };
    $scope.updatePubkey = function () {
    	if($scope.host.address == '') {
    		return;
    	}
        $scope.host.publicKey = '';
        $scope.pubkeyUpdating = true;
        if($scope.updateTimeout != null) {
            $timeout.cancel($scope.updateTimeout);
            $scope.pubkeyUptoDate = false;
        }
		$scope.errors = [];
        appsession.get('s/r/host/helpers/pubkey?address='+$scope.host.address)
            .success(function(pubkey) {
                $scope.pubkeyUptoDate = true;
                $scope.pubkey = pubkey;
                $scope.host.publicKey = pubkey.fingerprint;
		        $scope.pubkeyUpdating = false;
            })
            .error(function(error) {
            	$scope.errorHandler(error);
            	$scope.pubkeyUpdating = false;
            });
    };
    $scope.checkHostAddress = function() {
    	if($scope.host.address.length == 0) {
    		return;
    	}
		appsession.get('s/r/host/byaddress/'+$scope.host.address)
			.success(function(data) {
				if(data.length > 0) {
					$scope.errorHandler({'code':'UNIQ',message : 'Host with this address already registered'});
				}
			});
    };
    $scope.close = function() {
        $uibModalInstance.dismiss('cancel');
    };
	$scope.onKeyPress = function(event) {
		if(event.keyCode == 13) {
			$scope.addHost();
		}
	};
	$scope.toggleDedicated = function() {
		$scope.host.dedicated = !$scope.host.dedicated;
	};
    $scope.addHost = function () {
    	var onHostAdded = function() {
			$uibModalInstance.close();
		};
		var hostAddError = function(error) {
			$scope.errorHandler(error);
			$scope.inprg = false;
		};

		$scope.inprg = true;
    	if($scope.usepubkey) {
    		var hostInfo = {
				host : $scope.host,
				powerManagement : $scope.powerManagment
			};
    	    appsession.put('s/r/host/join-pubkey',hostInfo)
				.success(onHostAdded)
				.error(hostAddError);
    	} else {
    		var hostInfo = {
				host : $scope.host,
				powerManagement : $scope.powerManagment,
				password : $scope.password.password
			};
			appsession.put('s/r/host/join',hostInfo)
				.success(onHostAdded)
				.error(hostAddError);
    	}
    };

	$scope.anyPmOfType = function(type) {
		for(var i = 0; i < $scope.powerManagment.length; i++) {
			if($scope.powerManagment[i]["@type"] === type) {
				return true;
			}
		}
		return false;
	}

	$scope.removePmOfType = function(type) {
		var newPm = [];
		angular.forEach($scope.powerManagment, function(item) {
			if(item["@type"] !== type) {
				newPm.push(item);
			}
		});
		$scope.powerManagment = newPm;
	}

	$scope.toggleIpmi = function() {
		if($scope.anyPmOfType('ipmi')) {
			$scope.removePmOfType('ipmi')
		} else {
			$scope.powerManagment.push({
				"@type" : "ipmi",
				"address" : null,
				"username" : null,
				"password" : null
			});
		}
	};

    appsession.get('s/r/host/helpers/controller-pubkey').success(function(result) {
    	$scope.controllerKey = result;
    });
};