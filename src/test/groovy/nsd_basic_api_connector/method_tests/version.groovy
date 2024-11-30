package nsd_basic_api_connector.method_tests


import nsd_basic_api_connector.ApiTestUtils

def v = ApiTestUtils.getApi().version()
println(v)

