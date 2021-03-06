define(['./module', 'angular'], function(module, angular) {
  'use strict';

  module.service('ceventTypesService', CeventTypesService);

  CeventTypesService.$inject = ['biobankXhrReqService', 'domainEntityService'];

  /**
   * Service to access Collection Event Types.
   */
  function CeventTypesService(biobankXhrReqService, domainEntityService) {
    var service = {
      getAll      : getAll,
      get         : get,
      addOrUpdate : addOrUpdate,
      remove      : remove
    };
    return service;

    //-------

    function uri(studyId, ceventTypeId, version) {
      var result = '/studies';
      if (arguments.length <= 0) {
        throw new Error('study id not specified');
      } else {
        result += '/' + studyId + '/cetypes';

        if (arguments.length > 1) {
          result += '/' + ceventTypeId;
        }

        if (arguments.length > 2) {
          result += '/' + version;
        }
      }
      return result;
    }

    function getAll(studyId) {
      return biobankXhrReqService.call('GET', uri(studyId));
    }

    function get(studyId, collectionEventTypeId) {
      return biobankXhrReqService.call('GET', uri(studyId) + '?cetId=' + collectionEventTypeId);
    }

    function addOrUpdate(ceventType) {
      var cmd = {
        studyId:            ceventType.studyId,
        name:               ceventType.name,
        recurring:          ceventType.recurring,
        specimenGroupData:  ceventType.specimenGroupData,
        annotationTypeData: ceventType.annotationTypeData
      };

      angular.extend(cmd, domainEntityService.getOptionalAttribute(ceventType, 'description'));

      if (ceventType.id) {
        cmd.id = ceventType.id;
        cmd.expectedVersion = ceventType.version;
        return biobankXhrReqService.call('PUT', uri(ceventType.studyId, ceventType.id), cmd);
      } else {
        return biobankXhrReqService.call('POST', uri(ceventType.studyId), cmd);
      }
    }

    function remove(ceventType) {
      return biobankXhrReqService.call(
        'DELETE', uri(ceventType.studyId, ceventType.id, ceventType.version));
    }

  }

});
