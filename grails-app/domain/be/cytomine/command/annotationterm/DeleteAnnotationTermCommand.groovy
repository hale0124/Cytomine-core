package be.cytomine.command.annotationterm

import be.cytomine.command.Command
import be.cytomine.command.UndoRedoCommand
import grails.converters.JSON
import be.cytomine.ontology.AnnotationTerm
import be.cytomine.ontology.Annotation
import be.cytomine.ontology.Term
import be.cytomine.command.DeleteCommand
import java.util.prefs.BackingStoreException

class DeleteAnnotationTermCommand extends DeleteCommand implements UndoRedoCommand {

  boolean saveOnUndoRedoStack = true;

  def execute() {
    log.info "Execute"


    try {
      def postData = JSON.parse(postData)
      Annotation annotation = Annotation.get(postData.annotation)
      Term term = Term.get(postData.term)
      log.info "Delete annotation-term with annotation=" + annotation + " term=" + term
      AnnotationTerm annotationTerm = AnnotationTerm.findByAnnotationAndTerm(annotation,term)
      String id = annotationTerm.id
      def response = super.createDeleteMessage(id,annotationTerm,"AnnotationTerm",[id,annotation.id,term.name] as Object[])
      AnnotationTerm.unlink(annotationTerm.annotation, annotationTerm.term)
      return response
    } catch (NullPointerException e) {
      log.error(e)
      return [data: [success: false, errors: e.getMessage()], status: 404]
    } catch (BackingStoreException e) {
      log.error(e)
      return [data: [success: false, errors: e.getMessage()], status: 400]
    }
  }



  def undo() {
    log.info("Undo")
    def annotationTermData = JSON.parse(data)
    def annotation = Annotation.get(annotationTermData.annotation)
    def term = Term.get(annotationTermData.term)

    AnnotationTerm annotationTerm = AnnotationTerm.createAnnotationTermFromData(annotationTermData)
    annotationTerm = AnnotationTerm.link(annotationTermData.id,annotation, term)

    HashMap<String,Object> callback = new HashMap<String,Object>();
    callback.put("annotationID",annotation.id)
    callback.put("termID",term.id)
    callback.put("imageID",annotation.image.id)

    return super.createUndoMessage(
            annotationTerm,
            'AnnotationTerm',
            [id,annotation.id,term.name] as Object[],
            callback
    );
  }



  def redo() {
    log.info("Redo")
    def postData = JSON.parse(postData)
    Annotation annotation = Annotation.get(postData.annotation)
    Term term = Term.get(postData.term)

    AnnotationTerm annotationTerm = AnnotationTerm.findByAnnotationAndTerm(annotation,term)
    String id =  annotationTerm.id
    AnnotationTerm.unlink(annotationTerm.annotation, annotationTerm.term)

    HashMap<String,Object> callback = new HashMap<String,Object>();
    callback.put("annotationID",annotation.id)
    callback.put("termID",term.id)
    callback.put("imageID",annotation.image.id)

    return super.createRedoMessage(
            id,
            'AnnotationTerm',
            [id,annotation.id,term.name] as Object[],
            callback
    );
  }

}