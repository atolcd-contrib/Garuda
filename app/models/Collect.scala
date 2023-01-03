package models

import org.joda.time.DateTime
import twitter.ObservableFile

case class Collect(name: String, directory: String, var account: String, createdAt: DateTime = DateTime.now()) {
	var rules: Option[Seq[Rule]] = None
	var temporaryRules: Option[Seq[TemporaryRule]] = None
	var isActive: Boolean = false
	var observableFile: ObservableFile = new ObservableFile(None)
	
	
	/*
	Modules related
	 */
	def close(): Unit = {
		isActive = false
		observableFile.setNone()
	}
	
	/*
	Rules related
	 */
	def initRules(rules: Seq[Rule], temporaryRules: Seq[TemporaryRule]): Unit = {
		this.rules = Some(rules)
		this.temporaryRules = Some(temporaryRules)
	}
	
	def activeRules: Seq[Rule] = {
		rules.getOrElse(List[Rule]()).filter(_.isActive)
	}
	
	def nonActiveRules: Seq[Rule] = {
		rules.getOrElse(List[Rule]()).filterNot(_.isActive)
	}
	
	def addRule(rule: Rule): Boolean = {
		if (rules.isDefined) {
			rules = Some(rules.get :+ rule)
		}
		rules.isDefined
	}
	
	def addRules(rules: Seq[Rule]): Boolean = {
		if (this.rules.isDefined) {
			this.rules = Some(this.rules.get ++ rules)
		}
		this.rules.isDefined
	}
	
	def removeRules(rules: Seq[Rule]): Boolean = {
		if (this.rules.isDefined) {
			this.rules = Some(this.rules.get.filterNot(rule => rules.map(_.id).contains(rule.id)))
		}
		this.rules.isDefined
	}
	
	def addTemporaryRule(rule: TemporaryRule): Boolean = {
		if (temporaryRules.isDefined) {
			temporaryRules = Some(temporaryRules.get :+ rule)
		}
		temporaryRules.isDefined
	}
	
	def removeTemporaryRules(rules: Seq[TemporaryRule]): Boolean = {
		if (temporaryRules.isDefined) {
			temporaryRules = Some(temporaryRules.get.filterNot(rule => rules.map(_.id.get).contains(rule.id.get)))
		}
		temporaryRules.isDefined
	}
}

object CollectForm {
	/**
	 * Forms related
	 */
	import play.api.data.Forms._
	import play.api.data._
	
	case class CollectData(name: String, account: String)
	
	val form: Form[CollectData] = Form(
		mapping(
			"Name" -> nonEmptyText,
			"Account" -> nonEmptyText
		)(CollectData.apply)(CollectData.unapply)
	)
}
