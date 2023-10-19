package nsd_basic_api_connector.method_tests


import nsd_basic_api_connector.ApiTestUtils

String METACLASS = 'task$task'

ApiTestUtils.getApi().find(
        METACLASS,
        [:],
        ['title', 'UUID', 'number'],
        20,
        5
)
