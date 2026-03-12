import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "com.tick.magna"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            // BOM
            implementation(libs.androidx.compose.bom)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.napier)

            implementation(libs.coil3.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.sqldelight.runtime)
            implementation(libs.coroutines.extensions)
            implementation(libs.navigation.compose)

            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.ktor.client.android)
            implementation(libs.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.native.driver)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqlite.driver)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
    }
}

sqldelight {
    databases {
        create("MagnaDatabase") {
            packageName.set("com.tick.magna")
            srcDirs.setFrom("src/commonMain/sqldelight")
            dialect(libs.sqldelight.dialect)
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/migrations"))
            verifyMigrations.set(true)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.tick.magna.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.tick.magna"
            packageVersion = "1.0.0"
        }
    }
}
