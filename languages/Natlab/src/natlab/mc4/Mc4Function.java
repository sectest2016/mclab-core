package natlab.mc4;

import java.io.File;
import java.util.List;

import natlab.mc4.symbolTable.*;
import natlab.toolkits.analysis.varorfun.*;

import ast.Function;

/**
 * 
 * This object represents a function with mc4.
 * It stores the AST of a function, as well as a symbol table.
 * 
 */

public class Mc4Function {
    Function function;
    SymbolTable symbolTable;
    File source;
    String name;
    
    //constructor 
    //takes in ... an AST function ... builds first symbol table
    //first symbol table is just function or var
    public Mc4Function(Function function, File source) {
        this.function = function.fullCopy();
        this.source = source;
        this.symbolTable = new SymbolTable();
        this.name = function.getName();
        
        createSymbolTable();
    }
    
    //creates and puts first order approximation of the symbol table - variable or function
    private void createSymbolTable(){
        //perform variable or function analysis on function and get result
        VFPreorderAnalysis functionAnalysis = new VFPreorderAnalysis(this.function);
        functionAnalysis.analyze();        
        VFFlowset<String, ? extends VFDatum> flowset; 
        flowset = functionAnalysis.getFlowSets().get(function);
        Mc4.debug("function "+name+" variable or function analysis result:\n"+flowset);
        
        //go through all symbols, and put them in the symbol table
        //TODO the var analysis only captures variables - get those first...
        for (ValueDatumPair<String, ? extends VFDatum> pair : flowset.toList()){
            VFDatum vf = pair.getDatum();
            String name = pair.getValue();
                        
            if (vf.isExactlyFunction()){
                symbolTable.put(name, new UnknownFunction());
            } if (vf.isExactlyAssignedVariable() || vf.isExactlyVariable()){
                symbolTable.put(name, new UnknownVariable());
            } else {
                System.err.println("symbol "+name+" in "+name+" cannot be resolved as a function or variable");
            }
        }        
        //...the rest are assumed to be functions for now
        for (String name : function.getSymbols()){
            if (!symbolTable.containsKey(name)){
                symbolTable.put(name, new UnknownFunction());
            }
        }
        
        //TODO deal with errors
    }
    
    
    //getter methods
    public SymbolTable getSymbolTable(){ return symbolTable; }
    public String getName(){ return name; }
    public Function getAst(){ return function; }
    public File getFile(){ return source; }
    
    
    
    @Override
    public String toString() {
        return 
            "Function: "+name+
            "\nfile: "+source+
            "\nSymbol table:\n"+symbolTable+
            "\ncode:\n"+function.getPrettyPrinted();
    }
}


