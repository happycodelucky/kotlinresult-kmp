// swift-tools-version:6.0
import PackageDescription

// BEGIN KMMBRIDGE VARIABLES BLOCK (do not edit)
let remoteKotlinUrl = "https://github.com/happycodelucky/kotlinresult-kmp/releases/download/v1.0.0/KotlinresultKit.xcframework.zip"
let remoteKotlinChecksum = "9c47f9c2c80c96fbc3cbd44942bf1b694b5f63c8e30d7ffde2aea8c779126ec3"
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