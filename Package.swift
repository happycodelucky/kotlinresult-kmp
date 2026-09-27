// swift-tools-version:6.0
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://github.com/happycodelucky/kotlinresult-kmp/releases/download/v1.0.1/KotlinresultKit.xcframework.zip"
let remoteKotlinChecksum = "ac2405198ec8598d4072f2ec6d1fa94ce0a9ebe3222b11b5959bd3024a7244dd"
let packageName = "KotlinresultKit"
// END KMMBRIDGE BLOCK

let package = Package(
    name: packageName,
    platforms: [
        .iOS(.v18),
.macOS(.v15)
    ],
    products: [
        .library(
            name: packageName,
            targets: [packageName]
        ),
    ],
    targets: [
        .binaryTarget(
            name: packageName,
            url: remoteKotlinUrl,
            checksum: remoteKotlinChecksum
        )
        ,
    ]
)