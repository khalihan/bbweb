define(['../../module'], function(module) {
  'use strict';

  module.directive('ceventAnnotTypesPanel', ceventAnnotTypesPanel);

  /**
   *
   */
  function ceventAnnotTypesPanel() {
    var directive = {
      require: '^tab',
      restrict: 'E',
      scope: {
        study: '=',
        annotTypes: '=',
        annotTypesInUse: '='
      },
      templateUrl: '/assets/javascripts/admin/studies/annotationTypes/ceventAnnotTypesPanel.html',
      controller: 'CeventAnnotTypesPanelCtrl as vm'
    };
    return directive;
  }

});
