type FeatureEnv = {
  VITE_ENABLE_KAKAO_AUTH?: string;
};

const env = (import.meta as ImportMeta & { env?: FeatureEnv }).env ?? {};

export const FEATURES = {
  kakaoAuth: env.VITE_ENABLE_KAKAO_AUTH === 'true',
};
