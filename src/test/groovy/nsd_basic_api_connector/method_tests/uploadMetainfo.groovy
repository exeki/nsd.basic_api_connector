package nsd_basic_api_connector.method_tests


import nsd_basic_api_connector.ApiTestUtils

def file = ApiTestUtils.getMetainfoFile()
ApiTestUtils.getApi().uploadMetainfo(file.getText())

