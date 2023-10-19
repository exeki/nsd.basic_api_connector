package nsd_basic_api_connector.method_tests


import nsd_basic_api_connector.ApiTestUtils

String METACLASS = 'task$task'
String TEXT = 'testFile.txt'
String OBJECT_UUID = 'serviceCall$50175809'

ApiTestUtils.getApi().create(
        METACLASS,
        [
                'description': TEXT,
                'serviceCall': OBJECT_UUID
        ]
)

