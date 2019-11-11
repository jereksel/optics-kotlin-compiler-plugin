package com.ivianuu.debuglog

import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.utils.Printer

class MyPackageFragmentProvider(
    private val classes: List<KtLightClassBase>,
    private val module: ModuleDescriptor
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

    val clz = fqNameToClass[fqName] ?: return emptyList()

    return listOf(
        object: PackageFragmentDescriptorImpl(
            module,
            fqName
        ) {
          override fun getMemberScope(): MemberScope {
            val t = this

            val functions: List<SimpleFunctionDescriptorImpl> = emptyList()

            val properties = listOf(
                OpticsPropertyDescriptor(
                    t,
                    null,
                    Annotations.EMPTY,
                    Modality.FINAL,
                    Visibilities.PUBLIC,
                    false,
                    Name.identifier("myAwesomeProperty"),
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    SourceElement.NO_SOURCE,
                    false, false, false, false, false, false
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

                  getter.initialize(module.builtIns.intType)
                  initialize(getter, null)
                  setType(module.builtIns.intType, emptyList(), null, module.builtIns.string.thisAsReceiverParameter)
                }

            )

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
        .map { _fqName ->
          var localFqName = _fqName

          while(localFqName.parent() != fqName) {
            localFqName = localFqName.parent()
          }

          localFqName

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