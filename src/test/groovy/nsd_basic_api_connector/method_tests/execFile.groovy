package nsd_basic_api_connector.method_tests


import nsd_basic_api_connector.ApiTestUtils

String FILE_NAME = 'testScript.groovy'

ApiTestUtils.getApi().execFile(
        new File(getClass().getClassLoader().getResource(FILE_NAME).getFile())
)

