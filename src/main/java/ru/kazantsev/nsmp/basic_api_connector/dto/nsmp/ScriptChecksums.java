package ru.kazantsev.nsmp.basic_api_connector.dto.nsmp;

import java.util.List;

/**
 * Ответ при пуше скриптов smpsync
 */
@SuppressWarnings("unused")
public class ScriptChecksums   {
    public List<SrcChecksum> scripts;
    public List<SrcChecksum> modules;
    public List<ScriptCategory> scriptsCategories;
    public List<SrcChecksum> advimports;
}
