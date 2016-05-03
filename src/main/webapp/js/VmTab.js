
kerubApp.controller('VmTab', function($scope, $uibModal, $log, socket, appsession, size) {
	$scope.humanFriendlySize = size.humanFriendlySize;
	$scope.vms = [];
	$scope.refreshVms = function() {
		appsession.get('s/r/vm').success(function(result) {
			$scope.vms = result.result;
		});
	};
	$scope.refreshVms();
	socket.subscribe('/vm', function(msg) {
		if(msg.obj['@type'] == 'vm') {
			$scope.refreshVms();
		}
	}, 'VmTab');
	$scope.newVmForm = function () {
		var modalInstance = $uibModal.open({
		   templateUrl : 'NewVmWizard.html',
		   controller : NewVmWizard
        });
		modalInstance.result.then(function() {
		});

	}
});