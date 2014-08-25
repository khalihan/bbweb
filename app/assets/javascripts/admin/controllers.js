/**
 * Administration controllers.
 *
 */
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('admin.controllers', []);

  mod.controller('AdminCtrl', [
    '$scope', '$state', 'AdminService',
    function($scope, $state, AdminService) {
      $scope.page = {
        counts: {
          studies: 0,
          centres: 0,
          users: 0
        },
        studiesAdmin: function() {
          $state.go("admin.studies");
        },
        centresAdmin: function() {
          $state.go("admin.centres");
        },
        usersAdmin: function() {
          $state.go("admin.users");
        }
      };

      AdminService.aggregateCounts().then(function(counts) {
        $scope.page.counts.studies = counts.studies;
        $scope.page.counts.centres = counts.centres;
        $scope.page.counts.users   = counts.users;
      });
    }]);

  return mod;

});
