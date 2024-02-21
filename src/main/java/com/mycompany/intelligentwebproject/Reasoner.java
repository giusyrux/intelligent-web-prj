package com.mycompany.intelligentwebproject;

import java.util.*;
import java.io.*;
import guru.nidi.graphviz.engine.*;
import guru.nidi.graphviz.model.*;
import guru.nidi.graphviz.parse.Parser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
import org.semanticweb.owlapi.model.parameters.Imports;


public class Reasoner {
    
    
    /*FUNZIONI PER EFFETTUARE IL PARSING DEI CONCETTI PER RENDERLI LEGGIBILI*/
    public static HashSet<String> parseConcepts(Set<OWLClassExpression> concepts, boolean highlight, OWLClassExpression... args){
        
        HashSet<OWLClassExpression> copy = new HashSet<>(concepts);
        HashSet<String> stringConcepts = new HashSet<>();
        
        if(highlight){
            
            for(OWLClassExpression expr: args){
                
                copy.remove(expr); //Per evitare stringhe duplicate
                stringConcepts.add("<b>"+parseConcept(expr)+"</b>");   
            }
        }
        
        for(OWLClassExpression expr: copy){
            
            stringConcepts.add(parseConcept(expr));
        }
        return stringConcepts;
    }
    
    public static String parseConcept(OWLClassExpression C){
        
        //Top
        if (C.isOWLThing()) return "&#8868;";
        
        else if(C.isOWLNothing()) return "&#8869;";
        
        //Complemento
        else if (C instanceof OWLObjectComplementOf){
            
            String complement = new String("");
            OWLObjectComplementOf not = (OWLObjectComplementOf) C;
            complement+=parseConcept(not.getOperand());
            
            if(!not.getOperand().isClassExpressionLiteral() || (not.getOperand() instanceof OWLObjectComplementOf)) complement="&#40;"+complement+"&#41;";
            
            complement ="&#172;"+complement; 
            return complement;
        }
        
        //Concetto atomico
        else if(C.isClassExpressionLiteral()) return C.asOWLClass().getIRI().getFragment();
        
        //Esistenziale
        else if (C instanceof OWLObjectSomeValuesFrom){
            
           String exist = new String("");
           OWLObjectSomeValuesFrom e = (OWLObjectSomeValuesFrom) C;
           exist+=parseConcept(e.getFiller());
           
           if(!e.getFiller().isClassExpressionLiteral() || (e.getFiller() instanceof OWLObjectComplementOf)) exist="&#40;"+exist+"&#41;";
           
           exist ="&#8707;"+e.getProperty().asOWLObjectProperty().getIRI().getFragment()+"." + exist;  
           return exist;
        }
        
        //Universale
        else if (C instanceof OWLObjectAllValuesFrom){
            
           String univ = new String("");
           OWLObjectAllValuesFrom u = (OWLObjectAllValuesFrom) C;
           univ+=parseConcept(u.getFiller());
           
           if(!u.getFiller().isClassExpressionLiteral() || (u.getFiller() instanceof OWLObjectComplementOf)) univ="&#40;"+univ+"&#41;";
           
           univ ="&#8704;"+u.getProperty().asOWLObjectProperty().getIRI().getFragment()+"." + univ;  
           return univ;
        }
        
        //Unione
        else if(C instanceof OWLObjectUnionOf){
            
            boolean first = true;
            OWLObjectUnionOf i = (OWLObjectUnionOf) C;
            String or = new String("");
            
            for(OWLClassExpression disj: i.asDisjunctSet()){
                
                String expr = parseConcept(disj);
                if(!first)
                    if(!disj.isClassExpressionLiteral() || disj instanceof OWLObjectComplementOf)
                        or += " &#8746; &#40;"+expr+"&#41;";
                    else
                        or += " &#8746; "+expr;
                else{
                    if(!disj.isClassExpressionLiteral() || disj instanceof OWLObjectComplementOf)
                        or += "&#40;"+expr+"&#41;";
                    else
                        or += expr;
                    first = false;
                }        
            }
            return or;
        }
        
        //Intersezione
        else if(C instanceof OWLObjectIntersectionOf){
            
            boolean first = true;
            OWLObjectIntersectionOf i = (OWLObjectIntersectionOf) C;
            String and = new String("");
            
            for(OWLClassExpression conj: i.asConjunctSet()){
                
                String expr = parseConcept(conj);
                if(!first)
                    if(!conj.isClassExpressionLiteral() || conj instanceof OWLObjectComplementOf)
                        and += " &#8745; &#40;"+expr+"&#41;";
                    else
                        and += " &#8745; "+expr;
                else{
                    if(!conj.isClassExpressionLiteral() || conj instanceof OWLObjectComplementOf)
                        and += "&#40;"+expr+"&#41;";
                    else
                        and += expr;
                    first = false;
                }
            }
            return and;
        }
        return null;

    }
    /*FINE FUNZIONI PARSING CONCETTI*/
    
    /*FUNZIONI PER CREARE FILE RDF*/
    public static FileWriter prepareRDFVocabulary(){
        
        try {
            
            FileWriter myWriter = new FileWriter("./rdf.txt");
            
            myWriter.write("<?xml version='1.0'?>\n");
            myWriter.write("<rdf:RDF\n");
            
            /*Prefissi*/
            myWriter.write("xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
            myWriter.write("xmlns:logop=\"https://www.w3schools.com/#logicaloperators\"\n");
            myWriter.write("xmlns:node=\"https://www.w3schools.com/#node\"\n");
            myWriter.write(">\n");
            return myWriter;
            
        } 
        catch (Exception e){
            
            e.printStackTrace();
        }
        return null;
    }
    
    public static void createRDFNode(FileWriter rdfFile, int branchCount, OWLNamedIndividual x, int indent, Set<OWLClassExpression> Lx){
       
        try {
            
            rdfFile.write("\t".repeat(indent)+"<rdf:Description rdf:about='node:"+branchCount+"'>\n");
            rdfFile.write("\t".repeat(indent+1)+"<node:name>"+x.getIRI().getFragment()+"</node:name>\n");
            rdfFile.write("\t".repeat(indent+1)+"<node:LX>"+Lx+"</node:LX>\n"); 
        } 
        catch (Exception e){
            
            e.printStackTrace();
        }
    }
    
    public static void createLogPredicate(FileWriter rdfFile, int indent, String operator){
       
        try {
            
            if (operator.equals("or")) rdfFile.write("\t".repeat(indent)+"<logop:or>\n");
            
            else if (operator.equals("exists")) rdfFile.write("\t".repeat(indent)+"<logop:exists>\n");
        } 
        catch (Exception e){
            
            e.printStackTrace();
        }
    }
    
    public static void closeLogPredicate(FileWriter rdfFile, int indent, String operator){
       
        try {
            
            if (operator.equals("or")) rdfFile.write("\t".repeat(indent)+"</logop:or>\n");
            
            else if (operator.equals("exists")) rdfFile.write("\t".repeat(indent)+"</logop:exists>\n");
        } 
        catch (Exception e){
            
            e.printStackTrace();
        }
    }
    
    public static void createClashNode(FileWriter rdfFile, int branchCount, int indent){
       
        try {
        
           rdfFile.write("\t".repeat(indent)+"<node:clash> Clash "+branchCount+"</node:clash>\n"); 
        } 
        catch (Exception e){
            
            e.printStackTrace();
        }
    }
    
    public static void closeRDFNode(FileWriter file, int indent){
        
        try {
            file.write("\t".repeat(indent)+"</rdf:Description>\n"); 
        } 
        catch (Exception e) {
            
            e.printStackTrace();
        }
    }
    /*FINE FUNZIONI PER CREARE FILE RDF*/
    
    //funzione per creare il grafo.svg e la versione html
    public static void graphUpdate(FileWriter dotFile, FileWriter htmlFile, int [] branchCount, Set<OWLClassExpression> Lx, OWLClassExpression labels, String type, OWLNamedIndividual x, int father, OWLNamedIndividual z, Set<OWLClassExpression> LxChild, HashMap<OWLNamedIndividual,OWLObjectProperty> pairPropertyIndividual) throws IOException{
          
        if(type.equals("clash")){
            
            OWLClassExpression elem = labels;
            OWLClassExpression not_elem = labels.getComplementNNF();
            
            dotFile.write(branchCount[0]+" [label=\"clash\""+ " href=\"file:./labels.html"+"#"+Integer.toString(branchCount[0])+"\""+" target=\"_blank\"];\n");
            dotFile.write(branchCount[0]-1+"->"+branchCount[0]+";\n");
            htmlFile.write("\n\t<section id="+Integer.toString(branchCount[0])+">\n\t<h2> Clash </h2><h2 hidden> "+Integer.toString(branchCount[0])+": </h2>");
            htmlFile.write("\n\t\t"+parseConcepts(Lx, true, elem, not_elem).toString()+"\n</section>");  
        }
        else{
            
            String concepts = parseConcepts(Lx, false).toString();
        
            if(type.equals("blocking")){
            
                htmlFile.write("\n\t<section id="+Integer.toString(branchCount[0])+">\n\t<h2> Blocking </h2><h2 hidden> "+Integer.toString(branchCount[0])+": </h2>");
                htmlFile.write("\n\t\t"+x.getIRI().getFragment()+" bloccato da "+ z.getIRI().getFragment()+"\n</section>");
                dotFile.write(branchCount[0]+" [label=\"Blocking\""+" href=\"file:./labels.html"+"#"+Integer.toString(branchCount[0])+"\""+" target=\"_blank\"];\n");
                dotFile.write(branchCount[0]-1+"->"+branchCount[0]+";\n");  
            }
        
            if(type.equals("or")){

                dotFile.write(branchCount[0]+" [label=\""+x.getIRI().getFragment()+"\""+" href=\"file:./labels.html"+"#"+Integer.toString(branchCount[0])+"\""+" target=\"_blank\"];\n");
                dotFile.write(father+"->"+branchCount[0]+"[label=\""+parseConcept(labels)+"\"];\n");
                htmlFile.write("\n\t<section id="+Integer.toString(branchCount[0])+">\n\t<h2>L<sub>"+x.getIRI().getFragment()+"</sub> Nodo "+Integer.toString(branchCount[0])+": </h2>");
                htmlFile.write("\n\t\t"+concepts+"\n</section>");
            }

            if(type.equals("exists")){
                
                dotFile.write(branchCount[0]+" [label=\""+z.getIRI().getFragment()+"\""+" href=\"file:./labels.html"+"#"+Integer.toString(branchCount[0])+"\""+" target=\"_blank\"];\n");
                dotFile.write(father+"->"+branchCount[0]+"[label=\""+pairPropertyIndividual.get(z).getIRI().getFragment()+"\"];\n");
                htmlFile.write("\n\t<section id="+Integer.toString(branchCount[0])+">\n\t<h2>L<sub>"+z.getIRI().getFragment()+"</sub> Nodo "+Integer.toString(branchCount[0])+": </h2>");
                htmlFile.write("\n\t\t"+parseConcepts(LxChild, false).toString()+"\n</section>");
            }
        }
    }
    
    //funzione per trasformare i predicati dell'ontologia in relazioni di sussunzione tra classe e sottoclasse
    public static void transformToSubClasses(Set<OWLAxiom> TBox, OWLDataFactory df){
        
        for(OWLAxiom axiom: new HashSet<>(TBox)){
            
            if(axiom.getAxiomType().toString().equals("DisjointClasses")){
                
                for(OWLDisjointClassesAxiom ax: ((OWLDisjointClassesAxiom)axiom).asPairwiseAxioms()){
                    
                    TBox.add(df.getOWLSubClassOfAxiom(ax.getOperandsAsList().get(0), df.getOWLObjectComplementOf(ax.getOperandsAsList().get(1))));
                }
                TBox.remove(axiom);
            }
            
            if(axiom.getAxiomType().toString().equals("ObjectPropertyDomain")){
                
                OWLObjectPropertyDomainAxiom domain = (OWLObjectPropertyDomainAxiom) axiom;
                TBox.add(df.getOWLSubClassOfAxiom(df.getOWLObjectSomeValuesFrom(domain.getProperty(),df.getOWLThing()),domain.getDomain()));
                TBox.remove(axiom);
            }
            
            if(axiom.getAxiomType().toString().equals("ObjectPropertyRange")){
                
                OWLObjectPropertyRangeAxiom range = (OWLObjectPropertyRangeAxiom) axiom;
                TBox.add(df.getOWLSubClassOfAxiom(df.getOWLThing(), df.getOWLObjectAllValuesFrom(range.getProperty(), range.getRange())));
                TBox.remove(axiom);
            }
        }
    }
    
    //funzione per trasformare l'inclusione/equivalenza in unione
    public static OWLClassExpression transformInclusion(OWLAxiom axiom, OWLDataFactory df){     
        
        if(axiom.getAxiomType().toString().equals("SubClassOf")){
            
            OWLSubClassOfAxiom ax = (OWLSubClassOfAxiom) axiom;
            return df.getOWLObjectUnionOf(df.getOWLObjectComplementOf(ax.getSubClass()),ax.getSuperClass());
        }
        
        else if(axiom.getAxiomType().toString().equals("EquivalentClasses")){
               
            OWLObjectIntersectionOf intersection = null;
            for(OWLEquivalentClassesAxiom ax: ((OWLEquivalentClassesAxiom)axiom).asPairwiseAxioms()){
                
                List<OWLClassExpression> pairAxioms = ax.getOperandsAsList();               
                OWLObjectUnionOf firstUnion =  df.getOWLObjectUnionOf(df.getOWLObjectComplementOf(pairAxioms.get(0)),pairAxioms.get(1));
                OWLObjectUnionOf secondUnion =  df.getOWLObjectUnionOf(df.getOWLObjectComplementOf(pairAxioms.get(1)),pairAxioms.get(0));
                
                if(intersection == null) intersection = df.getOWLObjectIntersectionOf(firstUnion,secondUnion);
                else intersection = df.getOWLObjectIntersectionOf(firstUnion,secondUnion,intersection);
            }
            return intersection;
        }
        return df.getOWLThing();
    }

    //regole del lazy unfolding
    public static void lazyRules(Set<OWLClassExpression> Lx, Set<OWLAxiom> Tu, OWLDataFactory df, HashMap<OWLClassExpression, Set<OWLNamedIndividual>> conceptABox, OWLNamedIndividual x){
        
        List<OWLClassExpression> copy= new LinkedList<>(Lx);
        ListIterator<OWLClassExpression> iterator = copy.listIterator();
        
        while(iterator.hasNext()){
            
            OWLClassExpression labels = iterator.next();
            iterator.remove();
            
            boolean complement = false;
            OWLClassExpression expression = labels;

            if(labels.getClassExpressionType().toString().equals("ObjectComplementOf")){

                complement = true;
                expression = ((OWLObjectComplementOf)expression).getOperand();
            }
            
            if(labels.isClassExpressionLiteral()){

                for(OWLAxiom expr: Tu){

                    if(expr.getAxiomType().toString().equals("EquivalentClasses")){

                       OWLClassExpression leftOperand = ((OWLEquivalentClassesAxiom)expr).getOperandsAsList().get(0);
                       OWLClassExpression rightOperand = ((OWLEquivalentClassesAxiom)expr).getOperandsAsList().get(1);

                       if(leftOperand.equals(expression)){

                            //prima regola
                            if(!complement && !Lx.contains(rightOperand)){
                                
                                if(!conceptABox.containsKey(rightOperand.getNNF())) conceptABox.put(rightOperand.getNNF(), new HashSet<>());
                                
                                conceptABox.get(rightOperand.getNNF()).add(x);
                                Lx.add(rightOperand.getNNF());
                                iterator.add(rightOperand.getNNF());
                                    
                            }
                            
                            //seconda regola
                            else if(complement && !Lx.contains(rightOperand.getComplementNNF())) {
                                
                                if(!conceptABox.containsKey(rightOperand.getComplementNNF())) conceptABox.put(rightOperand.getComplementNNF(), new HashSet<>());
                                
                                conceptABox.get(rightOperand.getComplementNNF()).add(x);
                                Lx.add(rightOperand.getComplementNNF());
                                iterator.add(rightOperand.getComplementNNF());
                            }
                        }
                    }
                    
                    if(expr.getAxiomType().toString().equals("SubClassOf")){

                        OWLClassExpression leftOperand = ((OWLSubClassOfAxiom)expr).getSubClass();
                        OWLClassExpression rightOperand = ((OWLSubClassOfAxiom)expr).getSuperClass();

                        //terza regola
                        if(leftOperand.equals(expression) && !complement){
                            
                            if(!Lx.contains(rightOperand)){
                                
                                if(!conceptABox.containsKey(rightOperand.getNNF())) conceptABox.put(rightOperand.getNNF(), new HashSet<>());
                                
                                conceptABox.get(rightOperand.getNNF()).add(x);
                                Lx.add(rightOperand.getNNF());
                                iterator.add(rightOperand.getNNF());
                            }
                        }
                    }
                }
            }
            iterator = copy.listIterator();
        }
    }
    
    //concept unfolding per la verifica delle dipendenze
    public static boolean checkLiteral(OWLClassExpression expression, Set<OWLClassExpression> axiomSet){
       
        Set<OWLClassExpression> lazyLx = new HashSet<>(); //set di sottoformule che derivano dall'applicazione delle regole sul concetto

        for(OWLClassExpression labels: axiomSet){

            if(labels.getClassExpressionType().toString().equals("ObjectIntersectionOf")){
                
                lazyLx.addAll(labels.asConjunctSet());
                return checkLiteral(expression, lazyLx);
                
            }
            else if(labels.getClassExpressionType().toString().equals("ObjectUnionOf") ){

                lazyLx.addAll(labels.asDisjunctSet());
                return checkLiteral(expression, lazyLx);
               
            }
            else if(labels.getClassExpressionType().toString().equals("ObjectSomeValuesFrom") ){
                
                OWLObjectSomeValuesFrom exist = (OWLObjectSomeValuesFrom) labels;
                
                lazyLx.add(exist.getFiller());
                return checkLiteral(expression, lazyLx);
            }
            else if(labels.getClassExpressionType().toString().equals("ObjectAllValuesFrom") ){
                
                OWLObjectAllValuesFrom universal = (OWLObjectAllValuesFrom) labels;
                
                lazyLx.add(universal.getFiller());
                return checkLiteral(expression, lazyLx);
            }
            else if (expression.equals(labels)) return true;
        }
        return false;
    }
    
    //funzione per verificare che il grafo delle dipendenze è aciclico
    public static boolean isAcyclic(Set<OWLAxiom> Tu, OWLClassExpression labels, OWLClassExpression leftExpr, OWLClassExpression rightExpr){
        
        for(OWLAxiom axiom: Tu){

            if(axiom.getAxiomType().toString().equals("EquivalentClasses")){
                
                List<OWLClassExpression> equivalentAxiom = ((OWLEquivalentClassesAxiom)axiom).getOperandsAsList();
                
                if(labels == null) 
                    return isAcyclic(Tu, equivalentAxiom.get(0), equivalentAxiom.get(0), equivalentAxiom.get(1));
                
                else if(equivalentAxiom.get(0).equals(equivalentAxiom.get(1)) || equivalentAxiom.get(1).equals(labels)) 
                    return false;
                
                else if(!equivalentAxiom.get(0).equals(labels) && equivalentAxiom.get(0).equals(rightExpr))
                    return isAcyclic(Tu, labels, equivalentAxiom.get(1), equivalentAxiom.get(1));
                              
                else if(!equivalentAxiom.get(1).isClassExpressionLiteral()){

                    Set<OWLClassExpression> subClassSet = new HashSet<>();
                    subClassSet.add(equivalentAxiom.get(1));
                    
                    //concept unfolding nel caso in cui il concetto a destra non è atomico
                    if(checkLiteral(labels, subClassSet)) return false;
                }
                else return true;
            }
            
            if(axiom.getAxiomType().toString().equals("SubClassOf")){
                
                OWLSubClassOfAxiom subClassAxiom = (OWLSubClassOfAxiom) axiom;
                
                if(labels == null) 
                    return isAcyclic(Tu, subClassAxiom.getSubClass(), subClassAxiom.getSubClass(), subClassAxiom.getSuperClass());
                
                else if(subClassAxiom.getSubClass().equals(subClassAxiom.getSuperClass()) || subClassAxiom.getSuperClass().equals(labels)) 
                    return false;
                
                else if(!subClassAxiom.getSubClass().equals(labels) && subClassAxiom.getSubClass().equals(rightExpr))
                    return isAcyclic(Tu, labels, subClassAxiom.getSuperClass(), subClassAxiom.getSuperClass());
                              
                else if(!subClassAxiom.getSuperClass().isClassExpressionLiteral()){

                    Set<OWLClassExpression> subClassSet = new HashSet<>();
                    subClassSet.add(subClassAxiom.getSuperClass());
                    
                    //concept unfolding nel caso in cui il concetto a destra non è atomico
                    if(checkLiteral(labels, subClassSet)) return false;
                }
                else return true;
            }
        }
        return true;
    }
    
    //controllo per il lazy unfolding
    public static boolean isUnfoldable(Set<OWLAxiom> Tu, OWLDataFactory df, OWLAxiom axiom){

        OWLClassExpression axiomLeftOperand = null;
        OWLClassExpression leftOperand = null;

        /*Check concetto atomico a sinistra*/
        if(axiom.getAxiomType().toString().equals("EquivalentClasses")){

            axiomLeftOperand = ((OWLEquivalentClassesAxiom)axiom).getOperandsAsList().get(0);

            if(axiomLeftOperand != null && axiomLeftOperand.isClassExpressionLiteral() && !axiomLeftOperand.equals(df.getOWLThing())){

                for(OWLAxiom operand: Tu){

                    if(operand.getAxiomType().toString().equals("SubClassOf")) leftOperand = ((OWLSubClassOfAxiom)operand).getSubClass();
                    if(!operand.equals(axiom) && axiomLeftOperand.equals(leftOperand)) return false;
                }
            }
            else return false;
        }
        else if(axiom.getAxiomType().toString().equals("SubClassOf")){

            axiomLeftOperand = ((OWLSubClassOfAxiom)axiom).getSubClass();

            if(axiomLeftOperand.equals(df.getOWLThing())) return false;
        }

        /*Check dipendenze*/
        return isAcyclic(Tu, null, null, null);
    }
    
    //funzione per il lazy unfolding
    public static ArrayList<Set<OWLAxiom>> lazyUnfolding(Set<OWLAxiom> TBox, OWLDataFactory df){
        
        Set<OWLAxiom> Tu = new HashSet<>();
        Set<OWLAxiom> Tg = new HashSet<>();
        ArrayList<Set<OWLAxiom>> pairTuTg = new ArrayList<>();
        
        for(OWLAxiom axiom: TBox){
            
            Tu.add(axiom);
            
            if(!isUnfoldable(Tu, df, axiom)){

                Tu.remove(axiom);
                Tg.add(axiom);
            }
        }
        
        //lista da cui estrarre le due strutture
        pairTuTg.add(Tu);
        pairTuTg.add(Tg);
        
        return pairTuTg;
    }
    
    //regola dell'esistenziale da applicare
    public static HashMap<OWLNamedIndividual,OWLObjectProperty> checkExistential(Set<OWLClassExpression> Lx, Set<OWLClassExpression> LxChild, OWLClassExpression Tg, OWLClassExpression exist, HashMap<OWLClassExpression, Set<OWLNamedIndividual>> conceptABox, Set<OWLObjectPropertyAssertionAxiom> propertyABox, OWLNamedIndividual x, OWLDataFactory df, Set<OWLNamedIndividual> individuals, int[] count_ind){
        
        boolean found = false;//flag che uso per capire se ho trovato un individuo z che non esiste nella computazione
        OWLNamedIndividual z = null; //individuo che dovrò creare
        HashMap<OWLNamedIndividual,OWLObjectProperty> pairConceptIndividuals = null; //associazione tra individuo e labels, mentre set contiene i legami ruoli-individui
        
        if (exist.getClassExpressionType().toString().equals("ObjectSomeValuesFrom")){
            
            OWLObjectSomeValuesFrom expr = (OWLObjectSomeValuesFrom)exist;            
            OWLClassExpression concept = expr.getFiller(); //estraggo il filler
            OWLObjectProperty R = expr.getProperty().asOWLObjectProperty(); //estraggo il ruolo
            
            if(!conceptABox.containsKey(concept)) conceptABox.put(concept, new HashSet<>());
            
            for(OWLNamedIndividual ind: individuals){
                
                if(!x.equals(ind)){
                    
                    if(propertyABox.contains(df.getOWLObjectPropertyAssertionAxiom(R, x, ind)) && conceptABox.get(concept).contains(ind)){

                        found = true;
                        break;
                    }
                }
            }
            if(!found && z == null){
                
                count_ind[0]++;
                z = df.getOWLNamedIndividual("z"+count_ind[0]);
            }

            if(!found){ //se z non è in relazione con x e non esiste

                LxChild.add(concept);//aggiungo il concetto associato all'esistenziale
                LxChild.add(Tg); //aggiungo la TBox
                conceptABox.get(concept).add(z); //aggiungo l'associazione alla hashmap
                
                //aggiorno l'ABox in relazione al nuovo individuo z
                if(!conceptABox.containsKey(Tg)) conceptABox.put(Tg, new HashSet<>());
                conceptABox.get(Tg).add(z);
                
                propertyABox.add(df.getOWLObjectPropertyAssertionAxiom(R, x, z)); //aggiungo la relazione al set

                //ritorno l'associazione <individuo creato - ruolo che lo ha instanziato>
                pairConceptIndividuals = new HashMap<>(); //creo l'hashmap che associa l'individuo al ruolo
                pairConceptIndividuals.put(z, R);
            }
            
        }
        return pairConceptIndividuals; 
    }
    
    //regola dell'universale da applicare
    public static void checkUniversal(Set<OWLClassExpression> Lx, Set<OWLClassExpression> LxChild, OWLNamedIndividual x, OWLNamedIndividual z, OWLObjectProperty R, HashMap<OWLClassExpression, Set<OWLNamedIndividual>> conceptABox, Set<OWLObjectPropertyAssertionAxiom> propertyABox, OWLDataFactory df){
        
        for(OWLClassExpression labels: Lx){
           
            if(labels.getClassExpressionType().toString().equals("ObjectAllValuesFrom")){
                
                OWLObjectAllValuesFrom expr = (OWLObjectAllValuesFrom) labels;
                OWLClassExpression concept = expr.getFiller();
                OWLObjectProperty S = expr.getProperty().asOWLObjectProperty();

                //applico la regola dell'universale
                if(S.equals(R)){
                    
                    if(!conceptABox.containsKey(concept)) conceptABox.put(concept, new HashSet<>());
                    
                    if(!conceptABox.get(concept).contains(z)){

                        LxChild.add(concept);
                        conceptABox.get(concept).add(z);
                    }
                }
            }
        }
    }
    
    //funzione per verificare se sul ramo c'è un clash o meno
    public static boolean checkClash(Set<OWLClassExpression> Lx, OWLDataFactory df, FileWriter htmlFile, FileWriter dotFile, FileWriter rdfFile, int[] branchCount) throws Exception{

        for (OWLClassExpression labels: Lx){
            
            //controllo per ogni elemento se esiste il suo opposto nella lista, cioè clash
            if(Lx.contains(labels.getComplementNNF()) || labels.equals(df.getOWLNothing()) || labels.equals(df.getOWLObjectComplementOf(df.getOWLThing()))) {
                
                branchCount[0]++;
                graphUpdate(dotFile, htmlFile, branchCount, Lx, labels, "clash", null, 0, null, null, null);

                //ritorno true se trovo un clash
                return true;
            }
        }
        return false;
    }
    
    //funzione che struttura il tableaux e verifica la (in)soddisfacibilità del concetto
    public static boolean tableaux(OWLNamedIndividual x, Set<OWLClassExpression> Lx, OWLClassExpression Tg, Set<OWLAxiom> Tu, OWLDataFactory df, HashMap<OWLClassExpression, Set<OWLNamedIndividual>> conceptABox, Set<OWLObjectPropertyAssertionAxiom> propertyABox, Set<OWLNamedIndividual> individuals, HashMap<OWLNamedIndividual, Set<OWLClassExpression>> allLabels, int[] count_ind, FileWriter htmlFile, FileWriter dotFile, FileWriter rdfFile, int[] branchCount, int indent) throws Exception{
        
        boolean union = false;
        
        if(!allLabels.containsKey(x)) allLabels.put(x, Lx);
        
        HashMap<OWLNamedIndividual,OWLObjectProperty> pairPropertyIndividual; //mappa che uso per associare individui e ruoli 
        Set<OWLNamedIndividual> localndividuals;
        HashMap<OWLClassExpression,Set<OWLNamedIndividual>> localConceptABox;
        Set<OWLObjectPropertyAssertionAxiom> localPropertyABox;
        
        //verifico il criterio di blocking
        for(OWLNamedIndividual ancestor: allLabels.keySet()){
            
            if(!ancestor.equals(x) && allLabels.get(ancestor).containsAll(Lx)){
                
                System.out.println("BLOCKING:\n"+ancestor+" - "+allLabels.get(ancestor)+"\n"+x+" - "+Lx+"\n");
                
                int father = branchCount[0];
                branchCount[0]++;
                graphUpdate(dotFile, htmlFile, branchCount, Lx, null, "blocking", x, father, ancestor, null, null);

                return true;
            }
        }
       
        //esaustivamente analizzo tutti gli and
        for(OWLClassExpression labels: new HashSet<>(Lx)){
            
            if(labels.getClassExpressionType().toString().equals("ObjectIntersectionOf")){ //regola dell'intersezione

                int count = 0;
                
                //controllo l'ABox per capire se applicare l'and
                for(OWLClassExpression expr: labels.asConjunctSet()){

                    if(conceptABox.containsKey(expr) && conceptABox.get(expr).contains(x)) count++;
                }
                
                //L'ABox contiene parzialmente l'intersezione (contenimento stretto)
                if(count < labels.asConjunctSet().size()){
                    
                    for(OWLClassExpression expr: labels.asConjunctSet()){

                        if(!conceptABox.containsKey(expr))  conceptABox.put(expr, new HashSet<>());
                        conceptABox.get(expr).add(x);
                    }

                    //Lx.addAll(labels.asConjunctSet());
                    HashSet<OWLClassExpression> localConjuncts = new HashSet<>(labels.asConjunctSet());
                    List<OWLClassExpression> copy = new LinkedList<>(localConjuncts);
                    ListIterator<OWLClassExpression> iterator = copy.listIterator();
                    
                    //esaustivamente analizzo tutti i congiunti
                    while(iterator.hasNext()){
                        
                        OWLClassExpression it = iterator.next();
                        iterator.remove();
                        
                        if(it instanceof OWLObjectIntersectionOf){
                            
                            for(OWLClassExpression conj: it.asConjunctSet()){
                                
                                if(!conceptABox.containsKey(conj)) conceptABox.put(conj, new HashSet<>());
                                
                                if(!conceptABox.get(conj).contains(x)){
                                    localConjuncts.add(conj);
                                    iterator.add(conj); 
                                }
                            }  
                        }
                        iterator = copy.listIterator();
                    }
                    Lx.addAll(localConjuncts);
                }
            }
        }
        
        //applico le regole del lazy unfolding
        lazyRules(Lx, Tu, df, conceptABox, x);
        
        if(checkClash(Lx, df, htmlFile, dotFile, rdfFile, branchCount)){
            
            //AGGIORNO FILE RDF
            createClashNode(rdfFile, branchCount[0], indent);
            indent--;
            closeRDFNode(rdfFile, indent);
            return false;
        }

        //esaustivamente analizzo gli or
        for(OWLClassExpression labels: new HashSet<>(Lx)){
            
            if(labels.getClassExpressionType().toString().equals("ObjectUnionOf") ){ //regola dell'unione

                //controllo l'ABox per capire se applicare l'or
                union = true;
                for(OWLClassExpression expr: labels.asDisjunctSet()){

                    if(conceptABox.containsKey(expr) && conceptABox.get(expr).contains(x)){

                        //L'ABox e l'unione devono essere disgiunte
                        union = false;
                        break;
                    }
                }

                if(union){
                    
                    int father = branchCount[0];
                    
                    for(OWLClassExpression disjunct: labels.asDisjunctSet()){

                        if(!conceptABox.containsKey(disjunct))  conceptABox.put(disjunct, new HashSet<>());
                        
                        localndividuals = new HashSet<>(individuals);
                        localConceptABox = new HashMap<>();
                        localPropertyABox = new HashSet<>(propertyABox);
                        
                        for (OWLClassExpression key: conceptABox.keySet()) localConceptABox.put(key, new HashSet<>(conceptABox.get(key)));
                        
                        conceptABox.get(disjunct).add(x);
                        Lx.add(disjunct);
                        
                        //AGGIORNO FILE RDF
                        createLogPredicate(rdfFile, indent, "or");
                        indent++;
                        
                        //String concepts = parseConcepts(Lx, false).toString();
                        
                        branchCount[0]++;
                        createRDFNode(rdfFile, branchCount[0], x, indent, Lx);
                        indent++;
                        
                        //AGGIORNO GRAFO
                        graphUpdate(dotFile, htmlFile, branchCount, Lx, disjunct, "or", x, father, null, null, null);
                        
                        if(!tableaux(x, new HashSet<>(Lx), Tg, Tu, df, conceptABox, propertyABox, individuals, allLabels, count_ind, htmlFile, dotFile, rdfFile, branchCount, indent)){//primo ramo fallito, passo ad un altro
                            
                            //AGGIORNO FILE RDF
                            indent-=2;
                            closeLogPredicate(rdfFile, indent, "or");
                            
                            individuals = localndividuals;
                            conceptABox = localConceptABox;
                            propertyABox = localPropertyABox;
                            
                            allLabels.replace(x, Lx);
                            
                            //rimuovo il disgiunto che mi ha portato ad un clash e ne provo un altro
                            Lx.remove(disjunct);

                        }
                        else{
                            
                            //AGGIORNO FILE RDF
                            indent-=2;
                            closeLogPredicate(rdfFile, indent, "or");
                            indent--;
                            closeRDFNode(rdfFile, indent);
                            return true;
                        }
                    }
                    //AGGIORNO FILE RDF
                    indent--;
                    closeRDFNode(rdfFile, indent);
                    return false;
                }
            }
        }
        
        //ritorno i clash, se ve ne sono
        if(checkClash(Lx, df, htmlFile, dotFile, rdfFile, branchCount)){

            //AGGIORNO FILE RDF
            createClashNode(rdfFile, branchCount[0], indent);
            indent--;
            closeRDFNode(rdfFile, indent);
            return false;
        }
        
        int father = branchCount[0];

        //ciclo per gli esistenziali (e universali, se ve ne sono)
        for(OWLClassExpression labels: Lx){

            Set<OWLClassExpression> LxChild = new HashSet<>();
            
            //ritorno una coppia fatta dal nuovo individuo con il rispettivo ruolo che lo ha instanziato
            pairPropertyIndividual = checkExistential(Lx, LxChild, Tg, labels, conceptABox, propertyABox, x, df, individuals, count_ind);
            OWLNamedIndividual z = null;

            if(pairPropertyIndividual!=null){ //era presente una regola esistenziale con un individuo child

                z = pairPropertyIndividual.keySet().iterator().next(); //nuovo individuo                
                checkUniversal(Lx, LxChild, x, z, pairPropertyIndividual.get(z), conceptABox, propertyABox, df);
                
                localndividuals = new HashSet<>(individuals);
                localPropertyABox = new HashSet<>(propertyABox);
                localConceptABox = new HashMap<>();
                
                for (OWLClassExpression key: conceptABox.keySet()) localConceptABox.put(key, new HashSet<>(conceptABox.get(key)));
                
                //AGGIORNO FILE RDF
                createLogPredicate(rdfFile, indent, "exists");
                branchCount[0]++;
                indent++;
                
                //String concepts = parseConcepts(LxChild, false).toString();
                createRDFNode(rdfFile, branchCount[0], z, indent, LxChild);
                indent++;
                
                //AGGIORNO GRAFO
                graphUpdate(dotFile, htmlFile, branchCount, Lx, labels, "exists", x, father, z, LxChild, pairPropertyIndividual);
                
                if(!tableaux(z, new HashSet<>(LxChild), Tg, Tu, df, conceptABox, propertyABox, individuals, new HashMap<>(allLabels), count_ind, htmlFile, dotFile, rdfFile, branchCount, indent)){

                    //AGGIORNO FILE RDF
                    indent-=2;
                    closeLogPredicate(rdfFile, indent, "exists");
                    indent--;
                    closeRDFNode(rdfFile, indent);

                    individuals = localndividuals;
                    conceptABox = localConceptABox;
                    propertyABox = localPropertyABox;
                    
                    return false;
                }
                
                //AGGIORNO FILE RDF
                indent-=2;
                closeLogPredicate(rdfFile, indent, "exists");
            }
        }
        
        //ritorno il clash incontrato, altrimenti clash-free
        if(checkClash(Lx, df, htmlFile, dotFile, rdfFile, branchCount)){
            
            //AGGIORNO FILE RDF
            createClashNode(rdfFile, branchCount[0],indent);
            closeRDFNode(rdfFile, indent);
            return false;
        }
        else{

            //AGGIORNO FILE RDF
            indent--;
            closeRDFNode(rdfFile, indent);
            return true;
        }
    }
    
    public static void main(String[] args) {
      
        try {
            
            Set<OWLClassExpression> concept = new HashSet<>();
            Set<OWLObjectPropertyAssertionAxiom> propertyABox = new HashSet<>();
            HashMap<OWLClassExpression, Set<OWLNamedIndividual>> conceptABox = new HashMap<>();
            Set<OWLNamedIndividual> individuals = new HashSet<>();
            HashMap<OWLNamedIndividual,Set<OWLClassExpression>> allLabels = new HashMap<>();
            
            Set<OWLAxiom> TBox = new HashSet<>();
            Set<OWLClassExpression> tempTBox = new HashSet<>();
            OWLClassExpression Tg = null; 
            int[] count_ind = { 0 };
            
            final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            final OWLDataFactory df = manager.getOWLDataFactory(); 
            //final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File("./ontology_1.owl"));   // con solo il ruolo haComponente
            final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File("./ontology_2.owl")); // con in più il ruolo componenteDi
            
            //concept from prompt
            System.out.println("\nInserire (a capo) il concetto da verificare: "); 
            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();  
            
            //concept parsing in manchester
            ShortFormProvider sfp = new AnnotationValueShortFormProvider(Arrays.asList(df.getRDFSLabel()), Collections.<OWLAnnotationProperty, List<String>>emptyMap(), manager);
            BidirectionalShortFormProvider shortFormProvider = new BidirectionalShortFormProviderAdapter(manager.getOntologies(), sfp);
            
            ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();
            ShortFormEntityChecker owlEntityChecker = new ShortFormEntityChecker(shortFormProvider);
            
            parser.setOWLEntityChecker(owlEntityChecker);
            parser.setStringToParse(s);
            parser.setDefaultOntology(ontology);
            
            //inizializzazione concetto e tbox
            OWLClassExpression parsedConcept = parser.parseClassExpression();
            
            TBox.addAll(ontology.getTBoxAxioms(Imports.INCLUDED)); //riempio la TBox
            transformToSubClasses(TBox, df); //normalizzo la TBox
            
            //applico il lazy unfolding per generare Tu e Tg
            List<Set<OWLAxiom>> pairTuTg = lazyUnfolding(TBox, df);
            Set<OWLAxiom> Tu = pairTuTg.get(0); 
            Set<OWLAxiom> tempTg = pairTuTg.get(1);
            
            for (OWLAxiom axiom: tempTg) tempTBox.add(transformInclusion(axiom, df).getNNF());
            
            if(!tempTBox.isEmpty()){ 
                
                if(tempTBox.size() == 1)Tg = tempTBox.iterator().next();
                
                else Tg = df.getOWLObjectIntersectionOf(tempTBox);

            }
            else Tg = df.getOWLThing();
            
            //genero un primo individuo z0, gli altri saranno enumerati in modo crescente
            OWLNamedIndividual x = df.getOWLNamedIndividual("z0");
            individuals.add(x);
            
            concept.add(parsedConcept.getNNF());
            concept.add(Tg);
            
            //aggiorno l'ABox associando x al concetto
            conceptABox.put(parsedConcept.getNNF(), new HashSet<>());
            conceptABox.get(parsedConcept.getNNF()).add(x);
            
            System.out.println("\nConcetto: "+concept);
            System.out.println("\nTU: "+Tu);
            System.out.println("\nTG: "+Tg+"\n");
            
            //Inizializzo il grafo
            
            int[] branchCount = { 0 };
            FileWriter dotFile = new FileWriter("./graph.dot");
            dotFile.write("digraph G{\n");
            dotFile.write("0 [label=\""+x.getIRI().getFragment()+"\"];\n");
            
            //grafo HTML
            FileWriter htmlFile = new FileWriter("./labels.html");
            htmlFile.write("<!DOCTYPE HTML>\n<html>\n<head>\n\t<title>Lx List</title>\n\t<meta charset='utf-8'>\n</head>\n<body>\n\t<h1>Lista Lx</h1>\n");
            
            //nodo root del grafo
            htmlFile.write("\n\t<section id=\""+branchCount[0]+"\">\n\t<h2>L<sub>"+x.getIRI().getFragment()+"</sub> Radice </h2><h2 hidden>"+Integer.toString(branchCount[0])+": </h2>");
            htmlFile.write("\n\t\t"+parseConcepts(concept, false)+"\n</section>");      
            dotFile.write(branchCount[0]+" [label=\""+x.getIRI().getFragment()+"\""+" href=\"file:./labels.html"+"#"+Integer.toString(branchCount[0])+"\""+" target=\"_blank\"];\n");
            
            //file rdf
            FileWriter rdfFile = prepareRDFVocabulary();
            //nodo root rdf
            createRDFNode(rdfFile, branchCount[0], x, 0, concept);
 
            /*TABLEAUX*/

            //tempo di esecuzione del tableaux
            long startTime = System.nanoTime();
            
            if(tableaux(x, concept, Tg, Tu, df, conceptABox, propertyABox, individuals, allLabels, count_ind, htmlFile, dotFile, rdfFile, branchCount, 1)){ //rimettere new HashSet<>(Tu) al posto di null
                    System.out.println("\nIl concetto ["+s+"] rispetto alla TBox E' soddisfacibile.\n");
            }
            else{
                System.out.println("\nIl concetto ["+s+"] rispetto alla TBox NON E' soddisfacibile.\n");
            }

            long endTime = System.nanoTime();
            
            System.out.println("Tempo di esecuzione del reasoner: "+(endTime-startTime)/1000000+" ms\n\n");
            
            /*FINE TABLEAUX*/
            
            //chiudo i file rdf e graph
            dotFile.write("}");
            dotFile.close();
            htmlFile.write("</body>\n</html>\n");
            htmlFile.close();
            
            rdfFile.write("</rdf:RDF>\n"); 
            rdfFile.close();
            
            //salvo il grafo
            InputStream dot = new FileInputStream("./graph.dot");
            MutableGraph g = new Parser().read(dot);            
            Graphviz.fromGraph(g).totalMemory(33554432).width(1000).render(Format.SVG).toFile(new File("./graph.svg"));
                    
        }
        catch (Exception e){

            e.printStackTrace();
        }
    }
}