import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.api.artifacts.MinimalExternalModuleDependency

/** Looks up a library alias (dashes as in libs.versions.toml) from the shared version catalog. */
fun Project.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    extensions.getByType(VersionCatalogsExtension::class.java).named("libs").findLibrary(alias).get()
