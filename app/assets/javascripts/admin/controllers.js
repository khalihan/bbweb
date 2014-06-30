/**
 * Administration controllers.
 *
 */
define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('admin.controllers', []);

  mod.controller('AdminCtrl', [
    '$scope', '$location', 'userService',
    function($scope, $location, userService) {
  }]);

  return mod;

});
