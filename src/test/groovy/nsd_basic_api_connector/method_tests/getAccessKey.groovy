package nsd_basic_api_connector.method_tests

import nsd_basic_api_connector.ApiTestUtils

def api = ApiTestUtils.getApi()
println("ключ до " + api.accessKey)
def key = api.getAccessKey("system", "manager", 1)
println("полученный ключ " + key)
println("ключ после " + api.accessKey)