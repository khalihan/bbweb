define(['../module'], function(module) {
  'use strict';

  module.service('stateHelper', stateHelper);

  stateHelper.$inject = ['$state', '$stateParams'];

  /**
   * Utility function dealing with ui-router states.
   */
  function stateHelper($state, $stateParams) {
    var service = {
      reloadAndReinit: reloadAndReinit,
      reloadStateAndReinit: reloadStateAndReinit
    };

    return service;

    //-------

    /**
     * Hack for ui-router to reload state and re-initialize the controller.
     */
    function reloadAndReinit() {
      return $state.transitionTo(
        $state.current,
        $stateParams,
        { reload: true, inherit: false, notify: true });
    }

    function reloadStateAndReinit(stateName, stateParams, stateOptions) {
      var params = stateParams || {};
      var options = stateOptions || {};
      return $state.transitionTo(stateName, params, options);
    }
  }

});
