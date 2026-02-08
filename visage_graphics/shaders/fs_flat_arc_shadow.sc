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

float arcSignedDistance(vec2 normalized_position, float width, float thickness, float center_radians,
                        float radian_radius, float shape) {
  float radius = 0.5 * width;
  float scaled_thickness = 0.5 * thickness;
  float center_arc = radius - scaled_thickness;
  float delta_center = length(normalized_position) * radius;
  float delta_arc = delta_center - center_arc;
  float distance_arc = abs(delta_arc);

  float rads = mod(atan2(normalized_position.x, normalized_position.y) + center_radians, 2.0 * kPi) - kPi;
  float dist_curve_mult = min(delta_center, center_arc);
  float dist_curve_left = max(dist_curve_mult * (rads - radian_radius), 0.0);
  float dist_curve = max(dist_curve_mult * (-rads - radian_radius), dist_curve_left);
  float distance_from_thickness = mix(distance_arc, length(vec2(distance_arc, dist_curve)), shape);
  return distance_from_thickness - scaled_thickness;
}

void main() {
  vec2 offset = v_shader_values.xy;
  float blur_sigma = max(v_shader_values.z, 0.0);
  float spread = v_shader_values.w;
  float encoded_width = v_shader_values1.x;
  bool inset = encoded_width < 0.0;
  float width = max(abs(encoded_width), 0.0);
  float thickness = max(v_shader_values1.y, 0.0);
  float center_radians = v_shader_values1.z;
  float radians = min(v_shader_values1.w, kPi * 0.999);

  float shader_sigma = 2.0 * blur_sigma;
  float shader_spread = 2.0 * spread;
  vec2 shader_offset = 2.0 * offset;

  vec2 position = v_coordinates * v_dimensions;
  vec2 base_position = (position + shader_offset) / max(width, 0.0001);
  vec2 shadow_position = (position - shader_offset) / max(width, 0.0001);

  float base_distance = arcSignedDistance(base_position, width, thickness, center_radians, radians, 0.5);
  float shadow_distance = arcSignedDistance(shadow_position, width, thickness, center_radians, radians, 0.5);

  float source_distance = inset ? shadow_distance + shader_spread : shadow_distance - shader_spread;
  float source_alpha = gaussianMask(source_distance, shader_sigma);
  float base_alpha = 1.0 - smoothed(-1.0, 1.0, base_distance);

  float outer_alpha = source_alpha * (1.0 - base_alpha);
  float inner_alpha = base_alpha * (1.0 - source_alpha);
  float shadow_alpha = inset ? inner_alpha : outer_alpha;

  gl_FragColor = gradient(s_gradient, v_gradient_texture_pos, v_gradient_pos, v_gradient_pos2, v_position);
  gl_FragColor.a = gl_FragColor.a * clamp(shadow_alpha, 0.0, 1.0);
}
