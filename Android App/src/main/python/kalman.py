from math import sin, cos
from rtt import read_rtt, gen_rtt_line, gen_rtt_circle


def kalman(prev_mean_pose, prev_pose_cov, control, meas=False):
    # theta should be angle with x
    mu_x, mu_y, mu_theta = prev_mean_pose
    v_xx, v_yy, v_tt = prev_pose_cov
    step_len, heading = control
    # State transition Noise (variance)
    r_xx = r_yy = 0.167
    r_tt = 0.0158*heading
    # Measurement Noise (variance)
    q_xx = 0.5
    q_yy = 0.5
    # Prediction Step
    # Mean
    # mu_est = A*mu + B*u
    #   |mu_x_est    |   |1 0 0| |mu_x    |                                           |step_len |
    #  |mu_y_est    | = |0 1 0|*|mu_y    | + |cos(mu_theta_est) sin(mu_theta_est) 1|*|step_len |
    # |mu_theta_est|   |0 0 1| |mu_theta|                                           |delta-rot|
    mu_theta_est = (heading * 3.1415) / 180
    mu_x_est = mu_x + cos(mu_theta_est) * step_len  # Nonlinear?
    mu_y_est = mu_y + sin(mu_theta_est) * step_len
    # Covariance
    # cov_est = A*cov*A_t + R
    #   |v_xx_est|   |1 0 0| |v_xx 0 0| |1 0 0|T  |r_xx 0 0|
    #  |v_yy_est| = |0 1 0|*|0 v_yy 0|*|0 1 0| + |0 r_yy 0|
    # |v_tt_est|   |0 0 1| |0 0 v_tt| |0 0 1|   |0 0 r_tt|
    v_xx_est = v_xx + r_xx
    v_yy_est = v_yy + r_yy
    v_tt_est = v_tt + r_tt
    if meas is False:
        mu_x_est = round(mu_x_est, 3)
        mu_y_est = round(mu_y_est, 3)
        mu_theta_est = round(mu_theta_est, 3)
        v_xx_est = round(v_xx_est, 3)
        v_yy_est = round(v_yy_est, 3)
        v_tt_est = round(v_tt_est, 3)

        mean_pose_est = (mu_x_est, mu_y_est, mu_theta_est)
        pose_cov_est = (v_xx_est, v_yy_est, v_tt_est)
        return mean_pose_est, pose_cov_est
    else:
        z_x, z_y = read_rtt((mu_x_est, mu_y_est))
    # Measurement Update Step
    # Kalman Gain
    # K = cov_est*C_t*inv(C*cov_est*C_t + Q)
    #   |k_x 0  |   |v_xx_est 0 0| |1 0|   (|1 0 0| |v_xx_est 0 0| |1 0|)-1
    #  |0   k_y| = |0 v_yy_est 0|*|0 1| * (|0 1 0|*|0 v_yy_est 0|*|0 1|)
    # |0   0  |   |0 0 v_tt_est| |0 0|   (        |0 0 v_tt_est| |0 0|)
    k_x = v_xx_est / (v_xx_est + q_xx)
    k_y = v_yy_est / (v_yy_est + q_yy)
    # Mean
    # mu_new = mu_est + K*(z - C*mu_est)
    #   |mu_x_new    |   |mu_x_est    |   |k_x 0  | (|z_x|   |1 0 0| |mu_x_est    |)
    #  |mu_y_new    | = |mu_y_est    | + |0   k_y|*(|z_y| - |0 1 0|*|mu_y_est    |)
    # |mu_theta_new|   |mu_theta_est|   |0   0  | (                |mu_theta_est|)
    mu_x_new = mu_x_est + k_x*(z_x - mu_x_est)
    mu_y_new = mu_y_est + k_y*(z_y - mu_y_est)
    mu_theta_new = mu_theta_est
    # Variance
    # cov_new = (I - KC)*cov_est
    #   |v_xx_new 0 0|   (|1 0 0| |k_x 0  | |1 0 0|) |v_xx_est 0 0|
    #  |0 v_yy_new 0| = (|0 1 0|-|0   k_y|*|0 1 0|)*|0 v_yy_est 0|
    # |0 0 v_tt_new|   (|0 0 1| |0   0  |        ) |0 0 v_tt_est|
    v_xx_new = (1 - k_x)*v_xx_est
    v_yy_new = (1 - k_y)*v_yy_est
    v_tt_new = v_tt_est

    mu_x_new = round(mu_x_new, 3)
    mu_y_new = round(mu_y_new, 3)
    mu_theta_new = round(mu_theta_new, 3)
    v_xx_new = round(v_xx_new, 3)
    v_yy_new = round(v_yy_new, 3)
    v_tt_new = round(v_tt_new, 3)

    new_mean_pose = (mu_x_new, mu_y_new, mu_theta_new)
    new_pose_cov = (v_xx_new, v_yy_new, v_tt_new)
    return new_mean_pose, new_pose_cov


def kalman_init_line():
    global bel_pos, bel_cov
    init_bel_pos = 0, 0, 1.57
    init_bel_cov = 0.04, 0.04, 0.1
    bel_pos, bel_cov = init_bel_pos, init_bel_cov
    #gen_rtt_line(15, 3)
    gen_rtt_line(15, 2)
    return bel_pos, bel_cov


def kalman_init_circle():
    global bel_pos, bel_cov
    init_bel_pos = -5, 0, 1.57
    init_bel_cov = 0.04, 0.04, 0.1
    bel_pos, bel_cov = init_bel_pos, init_bel_cov
    gen_rtt_circle(5, 60)
    return bel_pos, bel_cov


def pdr(step_len, heading):
    global bel_pos, bel_cov
    step_len = step_len + 0.03697  # Bias correction
    heading = heading + 0.0661*heading  # Bias correction
    control = (step_len, heading)
    bel_pos, bel_cov = kalman(bel_pos, bel_cov, control)
    return bel_pos, bel_cov


def pdr_with_rtt(step_len, heading):
    global bel_pos, bel_cov
    step_len = step_len + 0.03697  # Bias correction
    heading = heading + 0.0661*heading  # Bias correction
    control = (step_len, heading)
    bel_pos, bel_cov = kalman(bel_pos, bel_cov, control, meas=True)
    return bel_pos, bel_cov