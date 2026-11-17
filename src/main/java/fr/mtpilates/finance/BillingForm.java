package fr.mtpilates.finance;

/** Dialog « Suivi financier » : source manuelle, montants, échéances et notes. */
public record BillingForm(String source, String contracted, String paid, String installments, String notes) {
}
