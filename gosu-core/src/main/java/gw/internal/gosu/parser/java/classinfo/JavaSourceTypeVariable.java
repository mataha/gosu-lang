/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser.java.classinfo;

import gw.internal.gosu.parser.ErrorType;
import gw.lang.parser.JavaClassTypeVarMatcher;
import gw.internal.gosu.parser.TypeLord;
import gw.internal.gosu.parser.TypeVariableType;
import gw.internal.gosu.parser.expressions.TypeVariableDefinitionImpl;
import gw.internal.gosu.parser.java.IJavaASTNode;
import gw.internal.gosu.parser.java.JavaASTConstants;
import gw.internal.gosu.parser.java.JavaParser;
import gw.lang.parser.TypeVarToTypeMap;
import gw.lang.reflect.IFunctionType;
import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeVariableType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.java.IJavaClassMethod;
import gw.lang.reflect.java.IJavaClassType;
import gw.lang.reflect.java.IJavaClassTypeVariable;
import gw.lang.reflect.java.ITypeInfoResolver;
import gw.lang.reflect.java.JavaTypes;
import gw.lang.reflect.module.IModule;

import java.util.List;

public class JavaSourceTypeVariable implements IJavaClassTypeVariable {
  public static final JavaSourceTypeVariable[] EMPTY = new JavaSourceTypeVariable[0];
  private IJavaASTNode boundNode;
  private String _name;
  private IJavaClassType[] _bounds;
  private ITypeInfoResolver _owner;

  private JavaSourceTypeVariable(ITypeInfoResolver owner, IJavaASTNode typeParameterNode) {
    _owner = owner;
    _name = typeParameterNode.getChildOfType(JavaParser.IDENTIFIER).getText();
    boundNode = typeParameterNode.getChildOfType(JavaASTConstants.typeBound);
  }

  public static IJavaClassTypeVariable create(ITypeInfoResolver owner, IJavaASTNode node) {
    if (isValid(node)) {
      return new JavaSourceTypeVariable(owner, node);
    } else {
      return new UnparseableSourceTypeVariable(owner);
    }
  }

  public static boolean isValid(IJavaASTNode typeParameterNode) {
    return typeParameterNode.getChildOfType(JavaParser.IDENTIFIER) != null;
  }

  @Override
  public IJavaClassType getConcreteType() {
    return getBounds()[0].getConcreteType();
  }

  @Override
  public String getNamespace() {
    return null;
  }

  public IJavaClassType[] getBounds() {
    if (_bounds == null) {
      if (boundNode != null) {
        List<IJavaASTNode> typeNodes = boundNode.getChildrenOfTypes(JavaASTConstants.type);
        _bounds = new IJavaClassType[typeNodes.size()];
        for (int i = 0; i < _bounds.length; i++) {
          _bounds[i] = JavaSourceType.createType(_owner, typeNodes.get(i));
        }
      } else {
        _bounds = new IJavaClassType[]{JavaTypes.OBJECT().getBackingClassInfo()};
      }
    }
    return _bounds;
  }

  public String getName() {
    return _name;
  }

  @Override
  public String getSimpleName() {
    return getName();
  }

  @Override
  public IModule getModule() {
    return _owner.getModule();
  }

  @Override
  public IType getActualType( TypeVarToTypeMap typeMap ) {
    return typeMap.getByMatcher( this, JavaClassTypeVarMatcher.instance() );
  }

  @Override
  public IType getActualType( TypeVarToTypeMap typeMap, boolean bKeepTypeVars ) {
    IType typeFromMap = typeMap.getByMatcher( this, JavaClassTypeVarMatcher.instance() );
    if( typeFromMap != null && !(typeFromMap instanceof ErrorType) ) {
      if( typeFromMap.getName().equals( getName() ) )
      {
        IType boundingType = ((ITypeVariableType)typeFromMap).getBoundingType();
        IType boundingTypeFromMap = TypeLord.getActualType( boundingType, typeMap, bKeepTypeVars );
        if( boundingType != boundingTypeFromMap )
        {
          TypeVariableDefinitionImpl tvd = ((TypeVariableDefinitionImpl)((ITypeVariableType)typeFromMap).getTypeVarDef()).clone( boundingTypeFromMap );
          typeFromMap = new TypeVariableType( tvd, ((ITypeVariableType)typeFromMap).getTypeVarDef().getEnclosingType() instanceof IFunctionType );
        }
      }
      return typeFromMap;
    }
    else {
      return TypeSystem.getErrorType( getName() );
    }
  }

  @Override
  public boolean isFunctionTypeVar() {
    return _owner instanceof IJavaClassMethod;
  }

  public String toString() {
    return getName() + " in " + _owner.toString();
  }
}
