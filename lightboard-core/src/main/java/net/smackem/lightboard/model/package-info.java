/**
 * This package contains the drawing model for lightboard.
 * The model must fulfill the following requirements:
 * <ul>
 *     <li>Mutability - figures are drawn in real-time</li>
 *     <li>Thread-safety - any time, a client may request the entire drawing</li>
 *     <li>Serializable to JSON</li>
 * </ul>
 */
package net.smackem.lightboard.model;