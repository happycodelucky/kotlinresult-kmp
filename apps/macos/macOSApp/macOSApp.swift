//
//  macOSApp.swift
//  __PROJECT_NAME__ macOS sample.
//
//  The content UI is shared with the iOS sample (apps/shared); this file is just
//  the macOS @main entry point.
//

import SwiftUI

@main
struct macOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .frame(minWidth: 480, minHeight: 360)
        }
        .windowResizability(.contentSize)
    }
}
