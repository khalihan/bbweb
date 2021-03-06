define(['../module'], function(module) {
  'use strict';

  module.controller('CentreEditCtrl', CentreEditCtrl);

  CentreEditCtrl.$inject = [
    '$scope',
    '$state',
    '$stateParams',
    'stateHelper',
    'centresService',
    'domainEntityUpdateError',
    'notificationsService',
    'user',
    'centre',
  ];

  /**
   *
   */
  function CentreEditCtrl($scope,
                          $state,
                          $stateParams,
                          stateHelper,
                          centresService,
                          domainEntityUpdateError,
                          notificationsService,
                          user,
                          centre) {
    var vm = this;
    var action = centre.id ? 'Update' : 'Add';
    var returnState = centre.id ? 'admin.centres.centre.summary' : 'admin.centres';

    vm.title = action + ' centre';
    vm.centre = centre;
    vm.submit = submit;
    vm.cancel = cancel;

    //---

    function gotoReturnState() {
      stateHelper.reloadStateAndReinit(returnState, $stateParams, {reload: true});
    }

    function submitSuccess() {
      notificationsService.submitSuccess();
      gotoReturnState();
    }

    function submit(centre) {
      centresService.addOrUpdate(centre)
        .then(submitSuccess)
        .catch(function(error) {
          domainEntityUpdateError(error, 'centre', returnState);
        });
    }

    function cancel() {
      gotoReturnState();
    }
  }

});
