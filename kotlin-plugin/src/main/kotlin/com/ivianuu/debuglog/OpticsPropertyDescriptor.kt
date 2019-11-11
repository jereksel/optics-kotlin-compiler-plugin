package com.ivianuu.debuglog

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.name.Name

class OpticsPropertyDescriptor(
    containingDeclaration: DeclarationDescriptor,
    original: PropertyDescriptor?,
    annotations: Annotations,
    modality: Modality,
    visibility: Visibility,
    isVar: Boolean,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    source: SourceElement,
    lateInit: Boolean,
    isConst: Boolean,
    isExpect: Boolean,
    isActual: Boolean,
    isExternal: Boolean,
    isDelegated: Boolean
) : OpticsSyntheticFunction, PropertyDescriptorImpl(
    containingDeclaration,
    original,
    annotations,
    modality,
    visibility,
    isVar,
    name,
    kind,
    source,
    lateInit,
    isConst,
    isExpect,
    isActual,
    isExternal,
    isDelegated
)