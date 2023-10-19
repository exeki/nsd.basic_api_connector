package nsd_basic_api_connector.method_tests


import nsd_basic_api_connector.ApiTestUtils

String OBJECT_UUID = 'serviceCall$50175809'

ApiTestUtils.getApi().get(OBJECT_UUID, ['title', 'UUID'])

