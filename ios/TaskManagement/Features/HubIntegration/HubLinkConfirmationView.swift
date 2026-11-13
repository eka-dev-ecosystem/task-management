import SwiftUI

public struct HubLinkSessionPayload: Identifiable, Sendable {
    public let id = UUID()
    public let sessionCode: String
    public let callbackURL: String

    public init(sessionCode: String, callbackURL: String = "ekahub://link-callback") {
        self.sessionCode = sessionCode
        self.callbackURL = callbackURL
    }
}

public struct HubLinkConfirmationView: View {
    public let payload: HubLinkSessionPayload
    public let onDismiss: () -> Void

    @State private var user: User?
    @State private var isLoading = false
    @State private var isFetchingProfile = true
    @State private var errorMessage: String?
    @State private var isSuccess = false

    private let apiClient = APIClient.shared
    private let keychain = KeychainManager.shared

    public init(payload: HubLinkSessionPayload, onDismiss: @escaping () -> Void) {
        self.payload = payload
        self.onDismiss = onDismiss
    }

    public var body: some View {
        NavigationStack {
            ZStack {
                Color(uiColor: .systemGroupedBackground)
                    .ignoresSafeArea()

                VStack(spacing: 24) {
                    Spacer().frame(height: 10)

                    // Hero Graphic
                    HStack(spacing: 16) {
                        BrandLogoView(size: 64)

                        Image(systemName: "arrow.left.arrow.right")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.secondary)

                        ZStack {
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .fill(LinearGradient(colors: [Color.blue, Color.purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(width: 64, height: 64)

                            Image(systemName: "sparkles")
                                .font(.system(size: 28, weight: .semibold))
                                .foregroundColor(.white)
                        }
                    }
                    .padding(.top, 16)

                    VStack(spacing: 8) {
                        Text("Hubungkan ke Personal AI Hub")
                            .font(.system(size: 22, weight: .bold))
                            .multilineTextAlignment(.center)

                        Text("Personal AI Hub meminta izin membaca ringkasan tugas dan status deadline Anda untuk asisten AI.")
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                    }

                    // Account Info Card
                    VStack(alignment: .leading, spacing: 12) {
                        Text("AKUN YANG AKAN DIHUBUNGKAN")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.secondary)
                            .tracking(0.5)

                        if isFetchingProfile {
                            HStack {
                                ProgressView()
                                    .padding(.trailing, 8)
                                Text("Memuat informasi akun...")
                                    .font(.system(size: 14))
                                    .foregroundColor(.secondary)
                                Spacer()
                            }
                            .padding(14)
                            .background(Color(uiColor: .secondarySystemGroupedBackground))
                            .cornerRadius(12)
                        } else if let user = user {
                            HStack(spacing: 12) {
                                ZStack {
                                    Circle()
                                        .fill(AppColors.primary.opacity(0.15))
                                        .frame(width: 44, height: 44)
                                    Text(String(user.name.prefix(1)).uppercased())
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(AppColors.primary)
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(user.name)
                                        .font(.system(size: 16, weight: .semibold))
                                    Text(user.email)
                                        .font(.system(size: 13))
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                            }
                            .padding(14)
                            .background(Color(uiColor: .secondarySystemGroupedBackground))
                            .cornerRadius(12)
                        } else {
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(.orange)
                                Text("Sesi masuk tidak ditemukan. Harap masuk ke Task Hub terlebih dahulu.")
                                    .font(.system(size: 13))
                                    .foregroundColor(.secondary)
                            }
                            .padding(14)
                            .background(Color(uiColor: .secondarySystemGroupedBackground))
                            .cornerRadius(12)
                        }
                    }
                    .padding(.horizontal, 20)

                    // Error Message if any
                    if let error = errorMessage {
                        HStack(alignment: .top, spacing: 8) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.red)
                            Text(error)
                                .font(.system(size: 13))
                                .foregroundColor(.red)
                            Spacer()
                        }
                        .padding(12)
                        .background(Color.red.opacity(0.1))
                        .cornerRadius(10)
                        .padding(.horizontal, 20)
                    }

                    Spacer()

                    // Action Buttons
                    VStack(spacing: 12) {
                        Button(action: handleExchangeLink) {
                            HStack {
                                if isLoading {
                                    ProgressView()
                                        .tint(.white)
                                        .padding(.trailing, 6)
                                }
                                Text(isLoading ? "Menghubungkan..." : "Izinkan & Hubungkan")
                                    .font(.system(size: 16, weight: .semibold))
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(user != nil ? AppColors.primary : Color.gray)
                            .foregroundColor(.white)
                            .cornerRadius(14)
                        }
                        .disabled(isLoading || user == nil)

                        Button(action: handleCancel) {
                            Text("Batal")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(.secondary)
                                .frame(maxWidth: .infinity)
                                .frame(height: 44)
                        }
                        .disabled(isLoading)
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 16)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Tutup") {
                        handleCancel()
                    }
                    .disabled(isLoading)
                }
            }
            .task {
                await loadUserProfile()
            }
        }
    }

    private func loadUserProfile() async {
        isFetchingProfile = true
        errorMessage = nil
        do {
            let profile = try await apiClient.getProfile()
            self.user = profile
        } catch {
            // Fallback: check cached values
            if let email = keychain.get(key: "user_email") {
                let name = keychain.get(key: "user_name") ?? "Pengguna Task Hub"
                self.user = User(
                    id: keychain.get(key: "user_id") ?? email,
                    name: name,
                    email: email,
                    avatarUrl: nil,
                    provider: nil,
                    googleBound: false,
                    appleBound: false,
                    timezone: nil,
                    dueDateWarningThreshold: 1,
                    emailNotificationsEnabled: true,
                    reminderSound: nil
                )
            } else {
                errorMessage = "Gagal memuat profil pengguna: \(error.localizedDescription)"
            }
        }
        isFetchingProfile = false
    }

    private func handleExchangeLink() {
        guard let user = user else { return }
        guard let token = keychain.get(key: "access_token") ?? SharedWidgetDataManager.shared.getAuthToken(), !token.isEmpty else {
            errorMessage = "Sesi token tidak ditemukan. Harap masuk ulang."
            return
        }

        isLoading = true
        errorMessage = nil

        Task {
            do {
                guard let endpoint = URL(string: "https://hub-api.eka-dev.cloud/api/v1/hub/link/exchange") else {
                    throw URLError(.badURL)
                }

                var request = URLRequest(url: endpoint)
                request.httpMethod = "POST"
                request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                request.setValue("application/json", forHTTPHeaderField: "Accept")

                let payloadBody: [String: Any] = [
                    "session_code": payload.sessionCode,
                    "app_id": "taskmanagement",
                    "app_name": "Task Management",
                    "connected_uid": user.id,
                    "connected_email": user.email,
                    "api_base_url": "https://be-task-hub.eka-dev.cloud",
                    "read_only_token": token,
                    "custom_url_scheme": "taskhub://"
                ]

                request.httpBody = try JSONSerialization.data(withJSONObject: payloadBody)

                let (data, response) = try await URLSession.shared.data(for: request)
                guard let httpResp = response as? HTTPURLResponse, (200...299).contains(httpResp.statusCode) else {
                    let respString = String(data: data, encoding: .utf8) ?? "Unknown server response"
                    throw NSError(domain: "HubExchange", code: (response as? HTTPURLResponse)?.statusCode ?? 500, userInfo: [NSLocalizedDescriptionKey: respString])
                }

                await MainActor.run {
                    isLoading = false
                    isSuccess = true
                    let callback = "\(payload.callbackURL)?status=success&app_id=taskmanagement"
                    if let cbURL = URL(string: callback) {
                        #if canImport(UIKit)
                        UIApplication.shared.open(cbURL)
                        #endif
                    }
                    onDismiss()
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = "Gagal menghubungkan ke Hub: \(error.localizedDescription)"
                }
            }
        }
    }

    private func handleCancel() {
        let callback = "\(payload.callbackURL)?status=cancelled&app_id=taskmanagement"
        if let cbURL = URL(string: callback) {
            #if canImport(UIKit)
            UIApplication.shared.open(cbURL)
            #endif
        }
        onDismiss()
    }
}

