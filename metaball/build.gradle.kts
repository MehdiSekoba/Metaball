plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "ir.mehdisekoba.metaball"
    compileSdk = 37

    defaultConfig {
        minSdk = 31
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation.core)
}
mavenPublishing {
    coordinates(
        groupId = "io.github.mehdisekoba",
        artifactId = "Metaball",
        version = "1.0.0"
    )

    pom {
        name.set("Metaball")
        description.set("A customizable Jetpack Compose library for fluid metaball animations, featuring organic liquid shapes, smooth transitions, and dynamic scroll-driven effects.")
        url.set("https://github.com/MehdiSekoba")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("mehdisekoba")
                name.set("Mehdi Sekoba")
                email.set("sekobamahdi@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/MehdiSekoba/Metaball")
            connection.set("scm:git:git://github.com/MehdiSekoba/Metaball.git")
            developerConnection.set("scm:git:ssh://git@github.com/MehdiSekoba/Metaball.git")
        }
    }
    publishToMavenCentral()
    signAllPublications()
}
