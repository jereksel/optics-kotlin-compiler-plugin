package com.ivianuu.debuglog

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.searches.AllClassesSearch
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.j2k.j2k
import org.jetbrains.kotlin.idea.stubindex.KotlinIndexUtil
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelClassByPackageIndex
import org.jetbrains.kotlin.idea.vfilefinder.KotlinClassFileIndex
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.DATA_KEYWORD
import org.jetbrains.kotlin.load.java.lazy.descriptors.ClassDeclaredMemberIndex
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.storage.StorageManager

class MyPackageFragmentProviderExtension : PackageFragmentProviderExtension {
  override fun getPackageFragmentProvider(
      project: Project,
      module: ModuleDescriptor,
      storageManager: StorageManager,
      trace: BindingTrace,
      moduleInfo: ModuleInfo?,
      lookupTracker: LookupTracker
  ): PackageFragmentProvider? {

    if (moduleInfo == null || moduleInfo !is LibraryInfo) {
      return null
    }

    println("Module: $moduleInfo")

    AllClassesSearch.search(moduleInfo.contentScope(), project)
        .findAll()
        .asSequence()
        .filterIsInstance<KtLightClassForDecompiledDeclaration>()
        .filter { it.kotlinOrigin?.hasModifier(DATA_KEYWORD) == true }
        .forEach {
          println("${it.name}")
        }

    FileTypeIndex.getFiles(KotlinFileType.INSTANCE, moduleInfo.contentScope())
        .asSequence()
        .mapNotNull { PsiManager.getInstance(project).findFile(it) }
        .filterIsInstance<KtFile>()
        .flatMap { it.classes.asSequence() }
        .filter { it.qualifiedName != null }
        .forEach { psiClass ->
          println("File: ${psiClass.qualifiedName}")
      }

    return null
  }
}