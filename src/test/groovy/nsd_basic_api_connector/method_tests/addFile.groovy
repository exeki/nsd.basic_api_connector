package nsd_basic_api_connector.method_tests

import nsd_basic_api_connector.ApiTestUtils
import org.apache.http.entity.ContentType

String OBJECT_UUID = 'root$801'

ApiTestUtils.getApi().addFile(
        OBJECT_UUID,
        new File(getClass().getClassLoader().getResource('testFile.txt').getFile())
)

//ApiTestUtils.getApi().addFileByStream(
//        OBJECT_UUID,
//        [
//                new File(getClass().getClassLoader().getResource('testFile.txt').getFile())
//        ]
//)

ApiTestUtils.getApi().addFile(
        OBJECT_UUID,
        new File(getClass().getClassLoader().getResource('testFile.txt').getFile()).getBytes(),
        'get.txt',
        null
)

