package com.ivianuu.debuglog

import com.intellij.openapi.project.Project
import com.ivianuu.debuglog.OpticsConst.lensClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer

class MyPackageFragmentProvider(
    private val classes: List<KtLightClassBase>,
    private val module: ModuleDescriptor,
    private val project: Project
) : PackageFragmentProvider {

  private val topLevel = FqName.topLevel(Name.identifier("arrowx"))

  private val fqNameToClass = classes
      .mapNotNull { clz -> clz.getKotlinFqName()?.let { name -> FqName.fromSegments(listOf("arrowx") + name.pathSegments().map { it.asString() }) to clz } }
      .toMap()

  override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {

    if (fqName == topLevel) {
      return listOf(EmptyPackageFragmentDescriptorImpl(module, fqName))
    }

    if (fqNameToClass[fqName] == null) {
      val s = getSubPackagesOf(fqName) { true }
      if (s.isNotEmpty()) {
        return listOf(EmptyPackageFragmentDescriptorImpl(module, fqName))
      }
    }

    val clz = fqNameToClass[fqName]?.kotlinOrigin ?: return emptyList()

    val constructor = clz.primaryConstructor!!
    println("Constructor: ${constructor.type()}")

    return listOf(
        object: PackageFragmentDescriptorImpl(
            module,
            fqName
        ) {

          override fun getSource(): SourceElement {
            return clz.toSourceElement()
          }

          override fun getMemberScope(): MemberScope {
            val t = this

            val functions: List<SimpleFunctionDescriptorImpl> = emptyList()

            val properties = constructor.valueParameters.mapIndexed { constructorIndex, parameter ->

              OpticsPropertyDescriptor(
                  t,
                  null,
                  Annotations.EMPTY,
                  Modality.FINAL,
                  Visibilities.PUBLIC,
                  false,
                  parameter.nameAsName!!,
                  CallableMemberDescriptor.Kind.SYNTHESIZED,
                  SourceElement.NO_SOURCE,
                  false, false, false, false, false, false,

                  parentClass = constructor.type()!!,
                  parameterName = parameter.nameAsName!!,
                  parameterClass = parameter.type()!!,

                  constructorParamIndex = constructorIndex,
                  numberOfConstructorParams = constructor.valueParameters.size,
                  constructorParameterClasses = constructor.valueParameters.map { it.type()!! }
              ).apply {

                val getter = object: OpticsSyntheticFunction, PropertyGetterDescriptorImpl(
                    this,
                    Annotations.EMPTY,
                    Modality.FINAL,
                    visibility,
                    false,
                    false,
                    false,
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    null,
                    SourceElement.NO_SOURCE
                ) {}

                val genericType = module.findClassAcrossModuleDependencies(ClassId.topLevel(lensClass))?.defaultType!!

                val typeParameterDescriptor = TypeParameterDescriptorImpl.createWithDefaultBound(
                    this,
                    Annotations.EMPTY,
                    false,
                    Variance.INVARIANT,
                    Name.identifier("A"),
                    0
                )

                println("Constructor: ${constructor.type()}")

                val left = KotlinTypeFactory.simpleType(
                    genericType,
                    arguments = listOf(
                        TypeProjectionImpl(typeParameterDescriptor.defaultType),
                        TypeProjectionImpl(typeParameterDescriptor.defaultType),
                        TypeProjectionImpl(constructor.type()!!),
                        TypeProjectionImpl(constructor.type()!!)
                    )
                )

                val right = KotlinTypeFactory.simpleType(
                    genericType,
                    arguments = listOf(
                        TypeProjectionImpl(typeParameterDescriptor.defaultType),
                        TypeProjectionImpl(typeParameterDescriptor.defaultType),
                        TypeProjectionImpl(parameter.type()!!),
                        TypeProjectionImpl(parameter.type()!!)
                    )
                )

                val extensionReceiver = ExtensionReceiver(this, left, null)
                val receiverParameterDescriptor =
                    ReceiverParameterDescriptorImpl(this, extensionReceiver, Annotations.EMPTY)

                getter.initialize(left)
                initialize(getter, null)
                setType(right, listOf(typeParameterDescriptor), null, receiverParameterDescriptor)
              }

            }

            return object: MemberScopeImpl() {

              override fun getContributedFunctions(
                  name: Name,
                  location: LookupLocation
              ): Collection<SimpleFunctionDescriptor> {
                return functions.filter { it.name == name }
              }

              override fun getContributedVariables(
                  name: Name,
                  location: LookupLocation
              ): Collection<PropertyDescriptor> {
                return properties.filter { it.name == name }
              }

              override fun getContributedDescriptors(
                  kindFilter: DescriptorKindFilter,
                  nameFilter: (Name) -> Boolean
              ): Collection<DeclarationDescriptor> {
                return (functions + properties)
                    .filter { kindFilter.accepts(it) && nameFilter(it.name) }
              }

              override fun printScopeStructure(p: Printer) {
                p.println(this::class.java.simpleName)
              }

            }

          }
        }
    )


  }

  override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
    if (fqName.isRoot) {
      return listOf(topLevel)
    }

    return fqNameToClass
        .keys
        .filter { it.isSubpackageOf(fqName) }
        .mapNotNull { _fqName ->
          var localFqName = _fqName

          while(!localFqName.isRoot && localFqName.parent() != fqName) {
            localFqName = localFqName.parent()
          }

          if (localFqName.isRoot) {
            null
          } else {
            localFqName
          }

        }
        .filter { nameFilter(it.shortName()) }
        .toSet()

//    if (fqName == topLevel) {
//      return fqNameToClass.keys
//    }
//
//    return emptyList()

  }
}