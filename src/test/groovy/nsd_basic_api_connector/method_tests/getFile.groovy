package nsd_basic_api_connector.method_tests

import nsd_basic_api_connector.ApiTestUtils
import ru.kazantsev.nsd.basic_api_connector.Connector
import ru.kazantsev.nsd.basic_api_connector.NsdDto

String FILE_UUID = 'file$164367724'
String OBJECT_UUID = 'serviceCall$50175809'


Connector api = ApiTestUtils.getApi()
NsdDto.FileDto fileDto = api.getFile(FILE_UUID)
if(fileDto != null) {
    File file = new File('C:\\Users\\ekazantsev\\Downloads\\' + fileDto.title)
    file.createNewFile()
    file.setBytes(fileDto.bytes)
    println('файл записан по пути ' + file.path)
} else println('пустой ответ')

//api.addFile(OBJECT_UUID, fileDto.bytes, fileDto.title)
