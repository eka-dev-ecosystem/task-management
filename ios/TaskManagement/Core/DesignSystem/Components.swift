import SwiftUI

public struct AppButton: View {
    let title: String
    let icon: String?
    let isLoading: Bool
    let action: () -> Void

    public init(
        title: String,
        icon: String? = nil,
        isLoading: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.isLoading = isLoading
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else {
                    if let icon = icon {
                        Image(systemName: icon)
                            .font(.system(size: 15, weight: .semibold))
                    }
                    Text(title)
                        .font(.system(size: 15, weight: .semibold))
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(AppColors.brandGradient)
            .foregroundColor(.white)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .shadow(color: AppColors.primary.opacity(0.35), radius: 8, x: 0, y: 4)
        }
        .buttonStyle(.plain)
        .disabled(isLoading)
    }
}

public struct PriorityBadge: View {
    let priority: String

    public init(priority: String) {
        self.priority = priority
    }

    private var meta: (icon: String, color: Color) {
        switch priority.lowercased() {
        case "urgent":
            return ("exclamationmark.triangle.fill", AppColors.priorityUrgent)
        case "high":
            return ("chevron.up.2", AppColors.priorityHigh)
        case "medium":
            return ("minus", AppColors.priorityMedium)
        case "low":
            return ("chevron.down", AppColors.priorityLow)
        default:
            return ("minus", AppColors.priorityLow)
        }
    }

    public var body: some View {
        HStack(spacing: 3) {
            Image(systemName: meta.icon)
                .font(.system(size: 9, weight: .bold))
            Text(L10n.Tasks.localizedPriority(priority).uppercased())
                .font(.system(size: 10, weight: .bold))
        }
        .foregroundColor(meta.color)
        .padding(.horizontal, 7)
        .padding(.vertical, 3.5)
        .background(meta.color.opacity(0.12))
        .overlay(
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .stroke(meta.color.opacity(0.25), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }
}

public struct StatusBadge: View {
    let status: String

    public init(status: String) {
        self.status = status
    }

    private var color: Color {
        switch status.lowercased() {
        case "done": return AppColors.statusDone
        case "in_progress": return AppColors.statusInProgress
        case "review": return AppColors.statusReview
        case "backlog": return AppColors.statusBacklog
        default: return AppColors.statusTodo
        }
    }

    public var body: some View {
        Text(L10n.Tasks.localizedStatus(status).uppercased())
            .font(.system(size: 10, weight: .bold))
            .foregroundColor(color)
            .padding(.horizontal, 7)
            .padding(.vertical, 3.5)
            .background(color.opacity(0.12))
            .overlay(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .stroke(color.opacity(0.25), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }
}

public struct ProjectPill: View {
    let name: String
    let hexColor: String?

    public init(name: String, hexColor: String? = nil) {
        self.name = name
        self.hexColor = hexColor
    }

    private var parsedColor: Color {
        if let hex = hexColor, let c = Color(hex: hex) {
            return c
        }
        return AppColors.primary
    }

    public var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "folder.fill")
                .font(.system(size: 9))
            Text(name)
                .font(.system(size: 10, weight: .semibold))
        }
        .foregroundColor(parsedColor)
        .padding(.horizontal, 6)
        .padding(.vertical, 3)
        .background(parsedColor.opacity(0.12))
        .overlay(
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .stroke(parsedColor.opacity(0.3), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }
}

public struct ProgressBar: View {
    let percent: Int

    public init(percent: Int) {
        self.percent = max(0, min(100, percent))
    }

    public var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.secondary.opacity(0.15))
                    .frame(height: 6)

                Capsule()
                    .fill(
                        LinearGradient(
                            colors: [AppColors.indigo, AppColors.sky],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: proxy.size.width * CGFloat(percent) / 100.0, height: 6)
                    .animation(.easeInOut(duration: 0.3), value: percent)
            }
        }
        .frame(height: 6)
    }
}

public struct TaskItemSkeleton: View {
    public init() {}

    public var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .fill(Color.secondary.opacity(0.15))
                .frame(width: 22, height: 22)

            VStack(alignment: .leading, spacing: 6) {
                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(Color.secondary.opacity(0.2))
                    .frame(width: 180, height: 14)

                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(Color.secondary.opacity(0.15))
                    .frame(width: 100, height: 10)
            }

            Spacer()

            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color.secondary.opacity(0.15))
                .frame(width: 50, height: 22)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color(uiColor: .secondarySystemGroupedBackground))
        )
        .redacted(reason: .placeholder)
    }
}

public struct TaskListSkeleton: View {
    let count: Int
    public init(count: Int = 6) {
        self.count = count
    }

    public var body: some View {
        VStack(spacing: 10) {
            ForEach(0..<count, id: \.self) { _ in
                TaskItemSkeleton()
            }
        }
    }
}

/// Adaptive navigation container for top-level tabs.
/// On iPhone, when a tab resides within UIKit's `UIMoreNavigationController` (tabs 4+),
/// wrapping content in an additional `NavigationStack` creates duplicate navigation bars
/// and double back buttons on pushed child views. This component avoids redundant nesting on iPhone.
public struct AdaptiveTabNavigationStack<Content: View>: View {
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        #if canImport(UIKit)
        if UIDevice.current.userInterfaceIdiom == .phone {
            content
        } else {
            NavigationStack {
                content
            }
        }
        #else
        NavigationStack {
            content
        }
        #endif
    }
}

// MARK: - Fluid Motion & Micro-interactions (Gojek / Grab Style)

public struct SpringBounceButtonStyle: ButtonStyle {
    public let scaleAmount: CGFloat
    public init(scaleAmount: CGFloat = 0.95) {
        self.scaleAmount = scaleAmount
    }

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? scaleAmount : 1.0)
            .opacity(configuration.isPressed ? 0.92 : 1.0)
            .animation(.interpolatingSpring(mass: 0.5, stiffness: 300, damping: 18), value: configuration.isPressed)
            .onChange(of: configuration.isPressed) { _, isPressed in
                if isPressed {
                    #if canImport(UIKit)
                    let generator = UIImpactFeedbackGenerator(style: .light)
                    generator.impactOccurred()
                    #endif
                }
            }
    }
}

public extension View {
    func springBounce() -> some View {
        self.buttonStyle(SpringBounceButtonStyle())
    }
}

// MARK: - Splash Screen (Gojek / Grab Style Fluid Animations)

/// Premium Gojek/Grab-inspired animated Splash Screen for Task Hub.
/// Features elastic spring logo entrance, concentric radar pulse waves, ambient glowing halo,
/// brand shimmer shine, and fluid exit morphing.
public struct SplashScreenView: View {
    public let onFinished: () -> Void

    @State private var logoScale: CGFloat = 0.1
    @State private var logoOpacity: Double = 0.0
    @State private var logoRotation: Double = -18.0

    // Concentric radar pulse rings
    @State private var pulseRing1Scale: CGFloat = 0.8
    @State private var pulseRing1Opacity: Double = 0.0
    @State private var pulseRing2Scale: CGFloat = 0.8
    @State private var pulseRing2Opacity: Double = 0.0
    @State private var pulseRing3Scale: CGFloat = 0.8
    @State private var pulseRing3Opacity: Double = 0.0

    // Text & Shimmer
    @State private var textOpacity: Double = 0.0
    @State private var textOffset: CGFloat = 16
    @State private var taglineOpacity: Double = 0.0
    @State private var taglineOffset: CGFloat = 12
    @State private var shimmerPhase: CGFloat = -1.0

    // Ambient floating glow
    @State private var ambientGlow1: CGFloat = 0.6
    @State private var ambientGlow2: CGFloat = 0.6

    // Exit transition
    @State private var isExiting: Bool = false

    public init(onFinished: @escaping () -> Void) {
        self.onFinished = onFinished
    }

    public var body: some View {
        ZStack {
            // 1. Deep luxury dark canvas with ambient color glows
            Color(red: 9/255, green: 13/255, blue: 22/255)
                .ignoresSafeArea()

            // Ambient background aurora spotlights
            GeometryReader { geo in
                ZStack {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [AppColors.indigo.opacity(0.35), Color.clear],
                                center: .center,
                                startRadius: 20,
                                endRadius: geo.size.width * 0.55
                            )
                        )
                        .frame(width: geo.size.width * 0.9, height: geo.size.width * 0.9)
                        .position(x: geo.size.width * 0.8, y: geo.size.height * 0.25)
                        .scaleEffect(ambientGlow1)
                        .blur(radius: 40)

                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [AppColors.sky.opacity(0.28), Color.clear],
                                center: .center,
                                startRadius: 20,
                                endRadius: geo.size.width * 0.5
                            )
                        )
                        .frame(width: geo.size.width * 0.8, height: geo.size.width * 0.8)
                        .position(x: geo.size.width * 0.2, y: geo.size.height * 0.7)
                        .scaleEffect(ambientGlow2)
                        .blur(radius: 45)
                }
            }

            // 2. Concentric Radar Pulse Rings (Gojek / Grab Signature Radar Waves)
            ZStack {
                Circle()
                    .stroke(
                        LinearGradient(
                            colors: [AppColors.sky.opacity(0.4), AppColors.indigo.opacity(0.1)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 2
                    )
                    .frame(width: 140, height: 140)
                    .scaleEffect(pulseRing1Scale)
                    .opacity(pulseRing1Opacity)

                Circle()
                    .stroke(
                        LinearGradient(
                            colors: [AppColors.primary.opacity(0.3), AppColors.violet.opacity(0.1)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.5
                    )
                    .frame(width: 140, height: 140)
                    .scaleEffect(pulseRing2Scale)
                    .opacity(pulseRing2Opacity)

                Circle()
                    .stroke(
                        LinearGradient(
                            colors: [AppColors.sky.opacity(0.25), Color.clear],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
                    .frame(width: 140, height: 140)
                    .scaleEffect(pulseRing3Scale)
                    .opacity(pulseRing3Opacity)
            }

            // 3. Central Brand Content
            VStack(spacing: 24) {
                Spacer()

                // Logo with spring pop & subtle 3D hover
                ZStack {
                    // Soft aura behind logo
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [AppColors.primary.opacity(0.3), AppColors.sky.opacity(0.15)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 120, height: 120)
                        .blur(radius: 20)

                    BrandLogoView(size: 96)
                        .shadow(color: AppColors.primary.opacity(0.6), radius: 24, x: 0, y: 12)
                }
                .scaleEffect(logoScale)
                .rotationEffect(.degrees(logoRotation))
                .opacity(logoOpacity)

                // Brand Title with Shimmer Mask
                VStack(spacing: 8) {
                    Text("Task Hub")
                        .font(.system(size: 34, weight: .black, design: .rounded))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [.white, Color(white: 0.9)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .tracking(1.2)
                        .overlay(
                            GeometryReader { textGeo in
                                Rectangle()
                                    .fill(
                                        LinearGradient(
                                            stops: [
                                                .init(color: .clear, location: 0),
                                                .init(color: .white.opacity(0.7), location: 0.5),
                                                .init(color: .clear, location: 1)
                                            ],
                                            startPoint: .leading,
                                            endPoint: .trailing
                                        )
                                    )
                                    .rotationEffect(.degrees(25))
                                    .offset(x: shimmerPhase * textGeo.size.width * 2)
                                    .blendMode(.screen)
                            }
                        )
                        .mask(
                            Text("Task Hub")
                                .font(.system(size: 34, weight: .black, design: .rounded))
                                .tracking(1.2)
                        )
                        .opacity(textOpacity)
                        .offset(y: textOffset)

                    // Tagline reveal
                    Text("Master Your Time, Own Your Day")
                        .font(.system(size: 13, weight: .semibold, design: .rounded))
                        .foregroundStyle(Color.white.opacity(0.6))
                        .tracking(0.6)
                        .opacity(taglineOpacity)
                        .offset(y: taglineOffset)
                }

                Spacer()

                // Sleek Pill Loading Bar at Bottom
                VStack(spacing: 8) {
                    HStack(spacing: 4) {
                        ForEach(0..<3) { i in
                            Circle()
                                .fill(AppColors.sky)
                                .frame(width: 6, height: 6)
                                .scaleEffect(shimmerPhase > CGFloat(i) * 0.4 ? 1.2 : 0.7)
                                .opacity(shimmerPhase > CGFloat(i) * 0.4 ? 0.9 : 0.3)
                                .animation(
                                    .easeInOut(duration: 0.6)
                                        .repeatForever(autoreverses: true)
                                        .delay(Double(i) * 0.15),
                                    value: shimmerPhase
                                )
                        }
                    }
                    .padding(.bottom, 24)
                }
                .opacity(taglineOpacity)
            }
        }
        .scaleEffect(isExiting ? 1.08 : 1.0)
        .opacity(isExiting ? 0.0 : 1.0)
        .onAppear {
            startAnimationSequence()
        }
    }

    private func startAnimationSequence() {
        // 1. Initial haptic feedback
        #if canImport(UIKit)
        let impact = UIImpactFeedbackGenerator(style: .medium)
        impact.prepare()
        #endif

        // 2. Logo elastic spring entrance
        withAnimation(.interpolatingSpring(mass: 0.8, stiffness: 120, damping: 11, initialVelocity: 4)) {
            logoScale = 1.0
            logoOpacity = 1.0
            logoRotation = 0.0
        }

        #if canImport(UIKit)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) {
            impact.impactOccurred()
        }
        #endif

        // 3. Ambient aurora breath
        withAnimation(.easeInOut(duration: 2.2).repeatForever(autoreverses: true)) {
            ambientGlow1 = 1.2
            ambientGlow2 = 1.15
        }

        // 4. Concentric radar wave pulses (Gojek / Grab signature)
        // Wave 1
        withAnimation(.easeOut(duration: 1.4).repeatForever(autoreverses: false)) {
            pulseRing1Scale = 2.4
            pulseRing1Opacity = 0.0
        }
        pulseRing1Opacity = 0.55

        // Wave 2 (delayed)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            withAnimation(.easeOut(duration: 1.4).repeatForever(autoreverses: false)) {
                pulseRing2Scale = 2.6
                pulseRing2Opacity = 0.0
            }
            pulseRing2Opacity = 0.45
        }

        // Wave 3 (delayed)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) {
            withAnimation(.easeOut(duration: 1.4).repeatForever(autoreverses: false)) {
                pulseRing3Scale = 2.8
                pulseRing3Opacity = 0.0
            }
            pulseRing3Opacity = 0.35
        }

        // 5. Title & Tagline appearance with smooth easing
        withAnimation(.easeOut(duration: 0.65).delay(0.35)) {
            textOpacity = 1.0
            textOffset = 0
        }

        withAnimation(.easeOut(duration: 0.65).delay(0.55)) {
            taglineOpacity = 1.0
            taglineOffset = 0
        }

        // 6. Shimmer sheen sweep
        withAnimation(.easeInOut(duration: 1.1).delay(0.6)) {
            shimmerPhase = 1.2
        }

        // 7. Finish after minimum splash time and smoothly morph exit
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.85) {
            withAnimation(.easeInOut(duration: 0.42)) {
                isExiting = true
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.42) {
                onFinished()
            }
        }
    }
}



