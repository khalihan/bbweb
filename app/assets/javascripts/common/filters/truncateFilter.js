define(['../module'], function(module) {
  'use strict';

  module.filter('truncate', truncateFactory);

  //truncate.$inject = [];

  /**
   * Truncates a string to a specified length. If the string passed in exceeds length, it is tructated at
   * length and then the 'end' string is appended to it.
   */
  function truncateFactory() {
    return trunctate;

    function trunctate(text, length, end) {
      if (isNaN(length)) {
        length = 10;
      }

      if (end === undefined) {
        end = '...';
      }

      if ((text.length <= length) || (text.length - end.length <= length)) {
        return text;
      } else {
        return String(text).substring(0, length-end.length) + end;
      }
    }
  }

});
