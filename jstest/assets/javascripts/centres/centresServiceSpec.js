// Jasmine test suite
define(['angular', 'angularMocks', 'underscore', 'biobankApp'], function(angular, mocks, _) {
  'use strict';

  describe('Service: centresService', function() {

    var centresService, httpBackend;
    var centreNoId = {
      name:         'CTR1',
      status:       'Disabled',
      version:      1,
      timeAdded:    '2014-10-20T09:58:43-0600'
    };
    var centre = angular.extend({id: 'dummy-id'}, centreNoId);

    function uri(centreId) {
      var result = '/centres';
      if (arguments.length > 0) {
        result += '/' + centreId;
      }
      return result;
    }

    beforeEach(mocks.module('biobankApp'));

    beforeEach(inject(function (_centresService_, $httpBackend) {
      centresService = _centresService_;
      httpBackend = $httpBackend;
    }));

    afterEach(function() {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
    });

    it('should have the following functions', function () {
      expect(angular.isFunction(centresService.list)).toBe(true);
      expect(angular.isFunction(centresService.get)).toBe(true);
      expect(angular.isFunction(centresService.addOrUpdate)).toBe(true);
      expect(angular.isFunction(centresService.enable)).toBe(true);
      expect(angular.isFunction(centresService.disable)).toBe(true);
      expect(angular.isFunction(centresService.studies)).toBe(true);
      expect(angular.isFunction(centresService.addStudy)).toBe(true);
      expect(angular.isFunction(centresService.removeStudy)).toBe(true);
    });

    it('list should return a list of centres containing one element', function() {
      httpBackend.whenGET(uri()).respond({
        status: 'success',
        data: [centre]
      });

      centresService.list().then(function(data) {
        //console.log(JSON.stringify(data));

        expect(data.length).toEqual(1);
        expect(_.isEqual(centre, data[0]));
      });

      httpBackend.flush();
    });

    it('get should return valid object', function() {
      httpBackend.whenGET(uri(centre.id)).respond({
        status: 'success',
        data: centre
      });

      centresService.get(centre.id).then(function(data) {
        expect(_.isEqual(centre, data));
      });

      httpBackend.flush();
    });

    it('should allow adding a centre', function() {
      var centre = {name: 'CTR1', description: 'test'};
      var expectedResult = {status: 'success', data: 'success'};;
      httpBackend.expectPOST(uri(), centre).respond(201, expectedResult);
      centresService.addOrUpdate(centre).then(function(reply) {
        expect(reply).toEqual('success');
      });
      httpBackend.flush();
    });

    function statusChangeShared(status, serviceFn) {
      var expectedCmd = { id: centre.id, expectedVersion: centre.version};
      var expectedResult = {status: 'success', data: 'success'};;
      httpBackend.expectPOST(uri(centre.id) + '/' + status, expectedCmd).respond(201, expectedResult);
      serviceFn(centre).then(function(reply) {
        expect(reply).toEqual('success');
      });
      httpBackend.flush();
    }

    it('should enable a centre', function() {
      statusChangeShared('enable', centresService.enable);
    });

    it('should disable a centre', function() {
      statusChangeShared('disable', centresService.disable);
    });

    it('should get the studies linked to a centre', function() {
      var studyId = 'a-study-id';
      httpBackend.whenGET(uri(centre.id) + '/studies').respond({
        status: 'success',
        data: [studyId]
      });

      centresService.studies(centre.id).then(function(data) {
        expect(data.length).toEqual(1);
        expect(data[0]).toEqual(studyId);
      });

      httpBackend.flush();
    });

    it('should add a study to a centre', function() {
      var studyId = 'a-study-id';
      var expectedCmd = {centreId: centre.id, studyId: studyId};
      var expectedResult = {status: 'success', data: 'success'};
      httpBackend.expectPOST(uri(centre.id) + '/studies/' + studyId, expectedCmd).respond(201, expectedResult);
      centresService.addStudy(centre.id, studyId).then(function(reply) {
        expect(reply).toEqual('success');
      });
      httpBackend.flush();
    });

    it('should remove a study to a centre', function() {
      var studyId = 'a-study-id';
      var expectedCmd = {centreId: centre.id, studyId: studyId};
      var expectedResult = {status: 'success', data: 'success'};
      httpBackend.expectDELETE(uri(centre.id) + '/studies/' + studyId).respond(201);
      centresService.removeStudy(centre.id, studyId);
      httpBackend.flush();
    });

  });

});
