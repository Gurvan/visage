$input v_coordinates, v_dimensions, v_shader_values, v_shader_values1, v_position, v_gradient_pos, v_gradient_pos2, v_gradient_texture_pos

#include <shader_include.sh>

SAMPLER2D(s_gradient, 0);

float gaussianMask(float distance, float sigma) {
  if (sigma <= 0.0)
    return 1.0 - step(0.0, distance);

  float x = distance / (1.41421356237 * sigma);
  float x_sign = sign(x);
  x = abs(x);
  float t = 1.0 / (1.0 + 0.3275911 * x);
  float y = (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t +
             0.254829592) * t;
  float erf_x = x_sign * (1.0 - y * exp(-x * x));
  return 0.5 * (1.0 - erf_x);
}

void main() {
  vec2 offset = v_shader_values.xy;
  float blur_sigma = max(v_shader_values.z, 0.0);
  float spread = v_shader_values.w;
  bool inset = v_shader_values1.x > 0.5;
  vec2 dimensions = max(v_shader_values1.yz, vec2(0.0, 0.0));
  float power = max(v_shader_values1.w, 0.1);

  float shader_sigma = 2.0 * blur_sigma;
  float shader_spread = 2.0 * spread;
  vec2 shader_offset = 2.0 * offset;

  vec2 position = v_coordinates * v_dimensions;
  vec2 base_position = position + shader_offset;
  vec2 shadow_position = position - shader_offset;

  float base_distance = sdSquircle(base_position, dimensions, power);
  float shadow_distance = sdSquircle(shadow_position, dimensions, power);

  float source_distance = inset ? shadow_distance + shader_spread : shadow_distance - shader_spread;
  float source_alpha = gaussianMask(source_distance, shader_sigma);
  float base_alpha = 1.0 - smoothed(-1.0, 1.0, base_distance);

  float outer_alpha = source_alpha * (1.0 - base_alpha);
  float inner_alpha = base_alpha * (1.0 - source_alpha);
  float shadow_alpha = inset ? inner_alpha : outer_alpha;

  gl_FragColor = gradient(s_gradient, v_gradient_texture_pos, v_gradient_pos, v_gradient_pos2, v_position);
  gl_FragColor.a = gl_FragColor.a * clamp(shadow_alpha, 0.0, 1.0);
}
