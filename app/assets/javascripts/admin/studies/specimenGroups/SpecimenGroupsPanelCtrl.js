define(['../../module', 'underscore'], function(module, _) {
  'use strict';

  module.controller('SpecimenGroupsPanelCtrl', SpecimenGroupsPanelCtrl);

  SpecimenGroupsPanelCtrl.$inject = [
    '$scope',
    '$state',
    '$stateParams',
    'panelService',
    'modalService',
    'specimenGroupsService',
    'specimenGroupModalService',
    'specimenGroupRemoveService'
  ];

  /**
   * A panel to display a study's specimen groups.
   */
  function SpecimenGroupsPanelCtrl($scope,
                                   $state,
                                   $stateParams,
                                   panelService,
                                   modalService,
                                   specimenGroupsService,
                                   specimenGroupModalService,
                                   specimenGroupRemoveService) {
    var vm = this;

    var helper = panelService.panel(
      'study.panel.specimenGroups',
      'admin.studies.study.specimens.groupAdd');

    vm.study                 = $scope.study;
    vm.specimenGroups        = $scope.specimenGroups;
    vm.specimenGroupIdsInUse = $scope.specimenGroupIdsInUse;
    vm.update                = update;
    vm.remove                = remove;
    vm.add                   = helper.add;
    vm.information           = information;
    vm.panelOpen             = helper.panelOpen;
    vm.panelToggle           = helper.panelToggle;

    vm.modificationsAllowed = vm.study.status === 'Disabled';

    vm.tableParams = helper.getTableParams(vm.specimenGroups);

    //--

    /**
     * Displays a specimen group in a modal.
     */
    function information(specimenGroup) {
      specimenGroupModalService.show(specimenGroup);
    }

    /**
     * Switches state to updte a specimen group.
     */
    function update(specimenGroup) {
      if (_.contains(vm.specimenGroupIdsInUse, specimenGroup.id)) {
        modalService.modalOk(
          'Specimen Group in use',
          'This specimen group cannot be modified because it is in use by either ' +
            'a collection event type or a specimen link type');
      } else {
        $state.go(
          'admin.studies.study.specimens.groupUpdate',
          { specimenGroupId: specimenGroup.id });
      }
    }

    function remove(specimenGroup) {
      specimenGroupRemoveService.remove(specimenGroup, vm.specimenGroupIdsInUse);
    }

  }

});
