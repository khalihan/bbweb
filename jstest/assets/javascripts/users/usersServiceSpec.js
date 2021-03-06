// Jasmine test suite
define(['angular', 'angularMocks', 'underscore', 'biobankApp'], function(angular, mocks, _) {
  'use strict';

  describe('Service: userService', function() {

    var usersService, httpBackend;
    var fakeToken = 'fake-token';
    var userNoId = {
      version:      1,
      timeAdded:    '2014-10-20T09:58:43-0600',
      name:         'testuser',
      email:        'testuser@test.com',
      avatarUrl:    null,
      status:       'Active'
    };
    var user = angular.extend({id: 'dummy-id'}, userNoId);

    function uri(userId) {
      var result = '/users';
      if (arguments.length >= 1) {
        result += '/' + userId;
      }
      return result;
    }

    beforeEach(mocks.module('biobankApp'));

    beforeEach(inject(function (_usersService_, $httpBackend) {
      usersService = _usersService_;
      httpBackend = $httpBackend;
    }));

    afterEach(function() {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
    });

    it('should have the following functions', function () {
      expect(angular.isFunction(usersService.getAllUsers)).toBe(true);
      expect(angular.isFunction(usersService.getUser)).toBe(true);
      expect(angular.isFunction(usersService.query)).toBe(true);
      expect(angular.isFunction(usersService.getUsers)).toBe(true);
      expect(angular.isFunction(usersService.add)).toBe(true);
      expect(angular.isFunction(usersService.updateName)).toBe(true);
      expect(angular.isFunction(usersService.updateEmail)).toBe(true);
      expect(angular.isFunction(usersService.updatePassword)).toBe(true);
      expect(angular.isFunction(usersService.passwordReset)).toBe(true);
      expect(angular.isFunction(usersService.activate)).toBe(true);
      expect(angular.isFunction(usersService.lock)).toBe(true);
      expect(angular.isFunction(usersService.unlock)).toBe(true);
    });

    function doLogin() {
      var credentials = {
        email: 'test@test.com',
        password: 'test'
      };
      httpBackend.expectPOST('/login', credentials).respond(201, fakeToken);

      httpBackend.whenGET('/authenticate').respond({
        status: 'success',
        data: [user]
      });

      usersService.login(credentials).then(function(data) {
        expect(_.isEqual(data, user));
      });
      httpBackend.flush();
    }

    it('should allow a user to login', function() {
      doLogin();
    });

    it('show allow a user to logout', function() {
      httpBackend.expectPOST('/logout').respond(201, 'success');
      usersService.logout();
      httpBackend.flush();
    });

    it('should return the user that is logged in', function() {
      doLogin();
      expect(_.isEqual(usersService.getUser(), user));
    });

    it('should return a user given a valid user ID', function() {
      httpBackend.whenGET(uri(user.id)).respond({
        status: 'success',
        data: user
      });
      usersService.query(user.id).then(function(data) {
        expect(_.isEqual(data, user));
      });
      httpBackend.flush();
    });

    it('should query for multiple users', function() {
      var query = 'test';
      var sort = 'email';
      var order = 'desc';

      httpBackend.whenGET(uri() + '?' + query + '&sort=' + sort + '&order=' + order).respond({
        status: 'success',
        data: [user]
      });

      usersService.getUsers(query, sort, order).then(function(data) {
        expect(data.length).toBe(1);
        expect(_.isEqual(data[0], user));
      });
      httpBackend.flush();
    });

    it('should allow adding a user', function() {
      var postResult = {status: 'success', data: 'success'};
      var cmd = {
        name:      user.name,
        email:     user.email,
        password:  user.password
      };
      httpBackend.expectPOST(uri(), cmd).respond(201, postResult);
      usersService.add(userNoId).then(function(reply) {
        expect(reply).toEqual('success');
      });
      httpBackend.flush();
    });

    it('should allow changing a password', function() {
      var postResult = {status: 'success', data: 'success'};
      httpBackend.expectPOST('/passreset', {email: user.email}).respond(201, postResult);
      usersService.passwordReset(user.email).then(function(data) {
        expect(data).toBe('success');
      });
      httpBackend.flush();
    });

    it('should allow a users name to be changed', function() {
      var expectedCmd = {
        id:              user.id,
        expectedVersion: user.version,
        name:            user.name
      };
      var postResult = {status: 'success', data: 'success'};
      httpBackend.expectPUT(uri(user.id) + '/name', expectedCmd).respond(201, postResult);
      usersService.updateName(user, user.name).then(function(data) {
        expect(data).toBe('success');
      });
      httpBackend.flush();
    });

    it('should allow a users email to be changed', function() {
      var expectedCmd = {
        id:              user.id,
        expectedVersion: user.version,
        email:           user.email
      };
      var postResult = {status: 'success', data: 'success'};
      httpBackend.expectPUT(uri(user.id) + '/email', expectedCmd).respond(201, postResult);
      usersService.updateEmail(user, user.email).then(function(data) {
        expect(data).toBe('success');
      });
      httpBackend.flush();
    });

    it('should allow a users password to be changed', function() {
      var expectedCmd = {
        id:              user.id,
        expectedVersion: user.version,
        password:        user.password
      };
      var postResult = {status: 'success', data: 'success'};
      httpBackend.expectPUT(uri(user.id) + '/password', expectedCmd).respond(201, postResult);
      usersService.updatePassword(user, user.password).then(function(data) {
        expect(data).toBe('success');
      });
      httpBackend.flush();
    });

    function userStatusChange(status, serviceFn) {
      var expectedCmd = { id: user.id, expectedVersion: user.version};
      var postResult = {status: 'success', data: 'success'};;
      httpBackend.expectPOST(uri(user.id) + '/' + status, expectedCmd).respond(201, postResult);
      serviceFn(user).then(function(reply) {
        expect(reply).toEqual('success');
      });
      httpBackend.flush();
    }

    it('should allow activating a user', function() {
      userStatusChange('activate', usersService.activate);
    });

    it('should allow locking a user', function() {
      userStatusChange('lock', usersService.lock);
    });

    it('should allow unlocking a user', function() {
      userStatusChange('unlock', usersService.unlock);
    });


  });

});


