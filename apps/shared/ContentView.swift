//
//  ContentView.swift
//  __PROJECT_NAME__ — shared SwiftUI, used by both the iOS and macOS samples.
//
//  Demonstrates consuming the Kotlin Multiplatform library from Swift via the
//  SKIE-enhanced XCFramework. `import __FRAMEWORK__` brings in the generated
//  Swift API; `Greeter` is the placeholder Kotlin class. SKIE bridges Kotlin
//  suspend functions to async/await and Flow to AsyncSequence — grow this view
//  into your real sample once the template is rendered.
//

import SwiftUI
import __FRAMEWORK__

struct ContentView: View {
    @State private var greeting: String = ""

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "swift")
                .font(.system(size: 48))
                .foregroundStyle(.tint)
            Text(greeting.isEmpty ? "…" : greeting)
                .font(.title2)
                .multilineTextAlignment(.center)
        }
        .padding()
        .onAppear {
            // Call the Kotlin library. `Greeter` and `greet()` come from the
            // generated __FRAMEWORK__ module.
            greeting = Greeter().greet()
        }
    }
}

#Preview {
    ContentView()
}
