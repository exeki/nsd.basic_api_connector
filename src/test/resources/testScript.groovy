import groovy.json.JsonBuilder
def list = utils.find('serviceCall', [:], sp.limit(20)).collect{
  return [
    'title' : it.title,
    'UUID' : it.UUID,
    'number' : it.number
  ]
}
return new JsonBuilder(list).toString()