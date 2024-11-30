package nsd_basic_api_connector.method_tests


import nsd_basic_api_connector.ApiTestUtils

def v = ApiTestUtils.getApi().metainfo(15000)
println(v)
def file = ApiTestUtils.getMetainfoFile()
file.setText(v, "UTF-8")
println(file.path)

