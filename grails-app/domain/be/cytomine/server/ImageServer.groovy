package be.cytomine.server

import be.cytomine.warehouse.Mime
import be.cytomine.SequenceDomain

class ImageServer extends SequenceDomain {

  String name
  String url
  String service
  String className

  static hasMany = [mimes:Mime, mis:MimeImageServer]

  static constraints = {
    name blank : false
    url  blank : false
    mimes nullable : true
  }

  String toString() {
    name
  }

  def mimes() {
		return mis.collect{it.Mime}
  }

  def getBaseUrl() {
    return url + service
  }
}
